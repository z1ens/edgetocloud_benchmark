/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dynamicpricing;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.producer.ProducerConfig;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Properties;

public class DynamicPricingJob {
	public static void main(String[] args) throws Exception {
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.disableOperatorChaining();

		ParameterTool parameters = ParameterTool.fromArgs(args);
		String kafkaBrokers = parameters.get("bootstrap.servers", "localhost:9092");
		int windowSizeSec = parameters.getInt("window-size", 30);

		// ==== Kafka Sources ====
		KafkaSource<String> riderSource = KafkaSource.<String>builder()
				.setBootstrapServers(kafkaBrokers)
				.setTopics("rider-requests")
				.setGroupId("rider-group")
				.setStartingOffsets(OffsetsInitializer.latest())
				.setValueOnlyDeserializer(new SimpleStringSchema())
				.build();

		KafkaSource<String> driverSource = KafkaSource.<String>builder()
				.setBootstrapServers(kafkaBrokers)
				.setTopics("driver-status")
				.setGroupId("driver-group")
				.setStartingOffsets(OffsetsInitializer.latest())
				.setValueOnlyDeserializer(new SimpleStringSchema())
				.build();

		KafkaSource<String> trafficSource = KafkaSource.<String>builder()
				.setBootstrapServers(kafkaBrokers)
				.setTopics("traffic-info")
				.setGroupId("traffic-group")
				.setStartingOffsets(OffsetsInitializer.latest())
				.setValueOnlyDeserializer(new SimpleStringSchema())
				.build();

		// ==== Kafka Sink ====
		Properties kafkaSinkProps = new Properties();
		kafkaSinkProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
		kafkaSinkProps.setProperty(ProducerConfig.ACKS_CONFIG, "1");

		FlinkKafkaProducer<String> kafkaSink = new FlinkKafkaProducer<>(
				"dynamic_price",
				new SimpleStringSchema(),
				kafkaSinkProps);

		// ==== Rider Stream ====
		DataStream<RiderRequest> riderStream = env.fromSource(
						riderSource,
						WatermarkStrategy.noWatermarks(), // <-- for raw String only
						"Rider Raw Source"
				).map(RiderRequestParser::parse)
				.filter(r -> r != null && r.getLocation() != null)
				.assignTimestampsAndWatermarks(
						WatermarkStrategy.<RiderRequest>forBoundedOutOfOrderness(Duration.ofSeconds(5))
								.withTimestampAssigner((event, ts) -> event.getTimestamp())
				);

		// ==== Driver Stream ====
		DataStream<DriverStatus> driverStream = env.fromSource(
						driverSource,
						WatermarkStrategy.noWatermarks(),
						"Driver Raw Source"
				).map(DriverStatusParser::parse)
				.filter(d -> d != null && d.getLocation() != null && d.isAvailable())
				.assignTimestampsAndWatermarks(
						WatermarkStrategy.<DriverStatus>forBoundedOutOfOrderness(Duration.ofSeconds(5))
								.withTimestampAssigner((event, ts) -> event.getTimestamp())
				);

		// ==== Traffic Stream ====
		DataStream<TrafficInfo> trafficStream = env.fromSource(
						trafficSource,
						WatermarkStrategy.noWatermarks(),
						"Traffic Raw Source"
				).map(TrafficInfoParser::parse)
				.filter(t -> t != null && t.getLocation() != null)
				.assignTimestampsAndWatermarks(
						WatermarkStrategy.<TrafficInfo>forBoundedOutOfOrderness(Duration.ofSeconds(5))
								.withTimestampAssigner((event, ts) -> event.getTimestamp())
				);


		// ==== Demand and Supply Aggregation ====
		DataStream<Tuple2<String, Long>> demandCount = riderStream
				.map(r -> new Tuple2<>(r.getLocation(), 1L))
				.returns(Types.TUPLE(Types.STRING, Types.LONG))
				.keyBy(t -> t.f0)
				.window(TumblingEventTimeWindows.of(Time.seconds(windowSizeSec)))
				.reduce((a, b) -> new Tuple2<>(a.f0, a.f1 + b.f1));

		DataStream<Tuple2<String, Long>> supplyCount = driverStream
				.map(d -> new Tuple2<>(d.getLocation(), 1L))
				.returns(Types.TUPLE(Types.STRING, Types.LONG))
				.keyBy(t -> t.f0)
				.window(TumblingEventTimeWindows.of(Time.seconds(windowSizeSec)))
				.reduce((a, b) -> new Tuple2<>(a.f0, a.f1 + b.f1));

		// ==== Join Demand and Supply ====
		DataStream<DemandSupply> demandSupplyStream = demandCount
				.join(supplyCount)
				.where(t -> t.f0)
				.equalTo(t -> t.f0)
				.window(TumblingEventTimeWindows.of(Time.seconds(windowSizeSec)))
				.apply((Tuple2<String, Long> demand, Tuple2<String, Long> supply) -> {
					long windowStart = System.currentTimeMillis();
					long windowEnd = windowStart + windowSizeSec * 60 * 1000;
					return new DemandSupply(demand.f0, windowStart, windowEnd, demand.f1, supply.f1);
				});

		// ==== Broadcast Traffic and Enrich ====
		MapStateDescriptor<String, Integer> trafficStateDescriptor =
				new MapStateDescriptor<>("traffic-map", String.class, Integer.class);

		BroadcastStream<TrafficInfo> trafficBroadcast = trafficStream.broadcast(trafficStateDescriptor);

		DataStream<EnrichedPricingInput> enrichedStream = demandSupplyStream
				.connect(trafficBroadcast)
				.process(new DemandSupplyEnricher(trafficStateDescriptor));

		// ==== Price Calculation ====
		DataStream<String> priceResult = enrichedStream
				.map(input -> {
					DemandSupply ds = input.getDemandSupply();
					double trafficFactor = input.getTrafficFactor();
					double basePrice = 10.0;
					double ratio = (double) ds.getDemandCount() / Math.max(ds.getSupplyCount(), 1);
					double nightFactor = LocalTime.now().isAfter(LocalTime.of(22, 0)) || LocalTime.now().isBefore(LocalTime.of(6, 0)) ? 1.5 : 1.0;
					double finalPrice = basePrice * ratio * nightFactor * trafficFactor;

					return String.format(
							"{\"location\":\"%s\",\"price\":%.2f,\"demand\":%d,\"supply\":%d}",
							ds.getLocation(), Math.round(finalPrice * 100.0) / 100.0, ds.getDemandCount(), ds.getSupplyCount());
				});

		priceResult.addSink(kafkaSink).name("Kafka Price Sink");

		env.execute("Dynamic Pricing Job");
	}
}