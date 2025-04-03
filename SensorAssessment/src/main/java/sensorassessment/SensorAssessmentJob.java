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

package sensorassessment;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import java.time.Duration;
import java.util.Properties;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

public class SensorAssessmentJob {
	public static void main(String[] args) throws Exception {
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.disableOperatorChaining(); // must be before any operator is added

		ParameterTool parameters = ParameterTool.fromArgs(args);
		String inputTopic = parameters.get("input-topic", "sensor-data");
		String outputTopic = parameters.get("output-topic", "processed-data");
		String kafkaBrokers = parameters.get("bootstrap.servers", "10.10.2.61:9092");
		int threshold = parameters.getInt("temperature-threshold", 50);
		int windowSizeSec = parameters.getInt("window-size", 30);

		// Create KafkaSource
		KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
				.setBootstrapServers(kafkaBrokers)
				.setTopics(inputTopic)
				.setGroupId("sensor-consumer-group-test" +  System.currentTimeMillis())
				.setStartingOffsets(OffsetsInitializer.latest())  // start from the beginning
				.setValueOnlyDeserializer(new SimpleStringSchema())
				.build();

		Properties kafkaProducerProps = new Properties();
		kafkaProducerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
		kafkaProducerProps.setProperty(ProducerConfig.ACKS_CONFIG, "1");
		kafkaProducerProps.setProperty(ProducerConfig.RETRIES_CONFIG, "5");
		kafkaProducerProps.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "false");

		// Use env.fromSource instead of addSource
		DataStream<String> rawStream = env.fromSource(
				kafkaSource,
				WatermarkStrategy.noWatermarks(), // choose not to use WatermarkStrategy
				"Kafka Source"
		);

		DataStream<SensorData> parsedStream = rawStream
				.map(SensorParser::parse)
				.name("Map[0]: Parse");

		DataStream<SensorData> filteredStream = parsedStream
				.filter(data -> {
					return data != null
							&& data.getTemperature() != null && data.getTemperature() < 50
							&& data.getVoltage() != null && data.getVoltage() < 2.8;
				}).name("Filter: Complex Conditions");

		DataStream<SensorData> timestampedStream = filteredStream
				.assignTimestampsAndWatermarks(
						WatermarkStrategy.<SensorData>forBoundedOutOfOrderness(Duration.ofSeconds(1))
								.withTimestampAssigner((event, timestamp) -> {
									long ts = event.getTimestamp();
									if (ts < 1000000000000L) ts *= 1000;
									return ts;
								})
								.withIdleness(Duration.ofSeconds(10))
				)
				.name("Assign Timestamps");

		KeyedStream<SensorData, Integer> keyedStream = timestampedStream.keyBy(SensorData::getMoteId);

		WindowedStream<SensorData, Integer, TimeWindow> windowedStream =
				keyedStream.window(TumblingEventTimeWindows.of(Time.seconds(windowSizeSec)));

		DataStream<SensorData> aggregated = windowedStream.reduce((ReduceFunction<SensorData>) (v1, v2) -> {
			long now = System.nanoTime();
			v1.setProcessingStartTime(now);

			// Generate and sort the array
			double[] arr = new double[1000];
			for (int i = 0; i < arr.length; i++) {
				arr[i] = Math.random() * (v1.getTemperature() + 1);
			}
			java.util.Arrays.sort(arr);

			// Mimic getting the outside API
			try {
				Thread.sleep(10); // 10ms sleep
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			double avg = (v1.getTemperature() + v2.getTemperature()) / 2;
			return new SensorData(
					v1.getTimestamp(),
					v1.getEpoch(),
					v1.getMoteId(),
					avg + arr[500] % 5, // add some noise
					null, null, null);
		}).name("Window:Complex Aggregation");

		DataStream<String> output = aggregated
				.map(data -> {
					String json = String.format(
							"{\"moteId\":%d,\"temp\":%.2f,\"ts\":%d}",
							data.getMoteId(), data.getTemperature(), data.getTimestamp()
					);

					int hash = 0;
					for (char c : json.toCharArray()) {
						hash += c * 13;
					}

					return json + " | hash:" + hash;
				}).name("Map[2]: JSON+Hash");

		FlinkKafkaProducer<String> kafkaSink = new FlinkKafkaProducer<>(outputTopic, new SimpleStringSchema(), kafkaProducerProps);
		output.addSink(kafkaSink).name("Kafka Sink");

		env.execute("Sensor Assessment Job");
	}
}