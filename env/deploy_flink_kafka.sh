#!/bin/bash

# Define hosts
JOBMANAGER_HOST="zs01"
TASKMANAGER_HOSTS=("zs02" "zs03" "zs05" "zs07" "zs08")
KAFKA_HOST="zf01"

FLINK_DIR="/flink/flink-1.20.1"
KAFKA_DIR="/flink/kafka_2.13-4.0.0"
LOCAL_PORT_FORWARD="8081"

echo "Starting Flink JobManager on ${JOBMANAGER_HOST}..."
ssh ${JOBMANAGER_HOST} "cd ${FLINK_DIR} && nohup ./bin/jobmanager.sh start"

echo "Restarting Flink TaskManagers..."
for HOST in "${TASKMANAGER_HOSTS[@]}"; do
    echo "Restarting TaskManager on ${HOST}..."
    ssh $HOST "cd ${FLINK_DIR} && ./bin/taskmanager.sh stop-all && ./bin/taskmanager.sh start"
done

echo "Pinging zs01 -> 10.10.10.8 for TaskManager sync..."
ssh ${JOBMANAGER_HOST} "ping -c 10 10.10.10.8"

echo "Setting up SSH port forwarding for Flink UI on localhost:${LOCAL_PORT_FORWARD}..."
# Run port forwarding in background
ssh -f -L ${LOCAL_PORT_FORWARD}:127.0.0.1:8081 -C -N ${JOBMANAGER_HOST}

echo "Restarting Kafka on ${KAFKA_HOST}..."
ssh ${KAFKA_HOST} "cd ${KAFKA_DIR} && ./bin/kafka-server-stop.sh && ./bin/kafka-server-start.sh -daemon config/server.properties"

echo "Kafka and Flink all set"
