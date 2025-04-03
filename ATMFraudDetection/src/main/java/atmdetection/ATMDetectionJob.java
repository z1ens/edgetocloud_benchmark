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

package atmdetection;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import java.time.Duration;
import java.util.Properties;

public class ATMDetectionJob {
	public static void main(String[] args) throws Exception {
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.disableOperatorChaining(); // must be before any operator is added

		ParameterTool parameters = ParameterTool.fromArgs(args);
		String inputTopic = parameters.get("input-topic", "atm-transactions");
		String outputTopic = parameters.get("output-topic", "atm-alerts");
		String kafkaBrokers = parameters.get("bootstrap.servers", "localhost:9092"); // If deploy on cloud, change the argument
		int windowSizeSec = parameters.getInt("window-size", 30);

		Properties kafkaConsumerProps = new Properties();
		kafkaConsumerProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
		kafkaConsumerProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "atm-group");
		kafkaConsumerProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

		Properties kafkaProducerProps = new Properties();
		kafkaProducerProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
		kafkaProducerProps.setProperty(ProducerConfig.ACKS_CONFIG, "1");
		kafkaProducerProps.setProperty(ProducerConfig.RETRIES_CONFIG, "5");
		kafkaProducerProps.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "false");

		FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<>(inputTopic, new SimpleStringSchema(), kafkaConsumerProps);

		DataStream<String> rawStream = env.addSource(kafkaConsumer).name("Kafka Source");

		DataStream<ATMTransaction> parsed = rawStream
				.map(ATMTransactionParser::parse)
				.filter(txn -> txn != null).name("Map[0]: ATM Transaction");

		DataStream<ATMTransaction> filtered = parsed
				.filter(new AmountThresholdFilter(1000.0)).name("Filter: High Amount");

		DataStream<ATMTransaction> enriched = filtered
				.map(new FraudScoreEnrichment("http://localhost:5002/predict")).name("Map: Add Fraud Score");

		DataStream<ATMTransaction> timestamped = enriched.assignTimestampsAndWatermarks(
				WatermarkStrategy.<ATMTransaction>forBoundedOutOfOrderness(Duration.ofSeconds(5))
						.withTimestampAssigner((event, timestamp) -> event.getTimestamp())
		);

		KeyedStream<ATMTransaction, String> keyed = timestamped.keyBy(ATMTransaction::getAccountId);
		// Create a CEP alert stream
		DataStream<String> cepAlerts = FraudPatternDetector.detectHighRiskSequence(keyed);
		cepAlerts.addSink(new FlinkKafkaProducer<>(outputTopic, new SimpleStringSchema(), kafkaProducerProps));
		WindowedStream<ATMTransaction, String, TimeWindow> windowed = keyed.window(TumblingEventTimeWindows.of(Time.seconds(windowSizeSec)));
		DataStream<String> suspicious = windowed.apply(new SuspiciousTransactionDetector());

		FlinkKafkaProducer<String> kafkaSink = new FlinkKafkaProducer<>(outputTopic, new SimpleStringSchema(), kafkaProducerProps);
		suspicious.addSink(kafkaSink);

		env.execute("ATM Fraud Detection Job");
	}
}

