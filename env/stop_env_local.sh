#!/bin/bash

echo "Stopping Flink Cluster..."
cd ~/Downloads/flink-1.20.0 || { echo "Flink directory not found!"; exit 1; }
bin/stop-cluster.sh
sleep 5

echo "Stopping Kafka Broker..."
cd ~/Downloads/kafka_2.13-3.4.0 || { echo "Kafka directory not found!"; exit 1; }
bin/kafka-server-stop.sh
sleep 5

echo "Stopping Zookeeper..."
bin/zookeeper-server-stop.sh
sleep 5

# Ensure all related processes are stopped
echo "Terminating remaining Kafka, Zookeeper, and Flink processes..."
pkill -f kafka.Kafka
pkill -f zookeeper
pkill -f org.apache.flink.runtime.dispatcher.StandaloneDispatcher

echo "All services stopped successfully!"
