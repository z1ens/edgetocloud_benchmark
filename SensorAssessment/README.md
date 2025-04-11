# Sensor Data Assessment Pipeline

This project is a streaming data pipeline built using Apache Flink and Apache Kafka. It ingests real-time sensor data, filters and aggregates it, then sends the results to a Kafka output topic.

## **Input Data**

The pipeline processes sensor data in the following format:

| Column | Description | Example |
| --- | --- | --- |
| `date` | Date in `yyyy-MM-dd` format | `2004-03-31` |
| `time` | Time in `HH:mm:ss.SSSSSS` format | `03:38:15.757551` |
| `epoch` | Epoch ID for each sensor | `2` |
| `moteId` | Sensor device ID (1-54) | `37` |
| `temperature` | Sensor temperature (°C) | `122.153` |
| `humidity` | Sensor humidity (%) | `-3.91901` |
| `light` | Sensor light intensity (Lux) | `11.04` |
| `voltage` | Sensor voltage (V) | `2.03397` |

## **Data Processing Workflow Overview**

1. **Parse Raw Data**:  Read CSV-formatted sensor data from Kafka input topic, parse each line into a structured `SensorData` object.
2. **Filter**: Discard invalid or abnormal data records (e.g., temperature ≥ 50°C, voltage ≥ 2.8V). Filtered-out records can optionally be sent to a separate Kafka topic for inspection.
3. **Timestamp Assignment and Watermarks**: Assign event time based on sensor timestamp (with millisecond normalization). Add watermarks with 1-second tolerance and 10-second idleness detection
4. **Key by Device (Mote ID)**: Group sensor data by Mote ID for per-device processing.
5. **Window Aggregation**: Apply tumbling event-time windows (e.g., every 30 seconds). 
6. **Within each window**: Simulate external latency with sleep; Sort a large random array to simulate computation; Calculate average temperature and inject some synthetic noise.
7. **Complex Calculation & Mapping**: Convert the result to a JSON-like string; Calculate a simple hash to mimic heavier downstream processing logic.
8. **Output to Kafka**: Send processed JSON messages with hash to the output Kafka topic.

## **Running the Pipeline**

### Prerequisites

- Apache Flink (version 1.20 or compatible)
- Kafka
  - Broker running at: 10.10.2.61:9092
  - Required topics:
    - sensor-data (input)
    - processed-data (output)
### Steps

1. Produce data by running the python script:

    ```bash
    python send_sensor_data_kafka.py
    ```

2. Build the project with Maven:

    ```bash
    mvn clean install
    ```
3. Start Kafka, Flink job manager and task managers.

4. Run Flink Job

    ```bash
    bin/flink run -c sensorassessment.SensorAssessmentJob \
    target/sensor-assessment-0.1.jar \
    --input-topic sensor-data \
    --output-topic processed-data \
    --bootstrap.servers 10.10.2.61:9092 \
    --window-size 30 \
    --temperature-threshold 80
    ```


---
