#!/bin/bash

# Navigate to Kafka directory
cd ~/Downloads/kafka_2.13-3.4.0 || { echo "Kafka directory not found!"; exit 1; }

echo "Starting Zookeeper..."
bin/zookeeper-server-start.sh config/zookeeper.properties &  # Run in background
sleep 5  # Wait for Zookeeper to start

echo "Starting Kafka Broker..."
bin/kafka-server-start.sh config/server.properties &  # Run in background
sleep 10  # Wait for Kafka to start

echo "Checking available Kafka topics..."
bin/kafka-topics.sh --list --bootstrap-server localhost:9092

# Navigate to Flink directory
cd ~/Downloads/flink-1.20.0 || { echo "Flink directory not found!"; exit 1; }

echo "Starting Flink cluster..."
bin/start-cluster.sh
sleep 5  # Give time for Flink to initialize

echo "Environment setup complete!"
