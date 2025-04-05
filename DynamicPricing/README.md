# Dynamic Pricing Stream Pipeline with Apache Flink & Kafka

## Overview
This project implements a real-time **dynamic pricing system** for ride-hailing services, inspired by Uber's surge pricing logic. The architecture leverages **Apache Flink** for stream processing and **Apache Kafka** for ingesting and distributing event streams.

The pipeline is structured to simulate an edge-to-cloud system where pricing decisions are made based on real-time user demand, driver availability, and traffic conditions.

---

## Architecture

The pipeline ingests three streams:

- `rider_requests`: ride request events from users
- `driver_status`: updates on available drivers
- `traffic_info`: congestion levels by location

The pipeline performs the following steps:

1. **Parse & Clean** incoming JSON events into POJOs (`RiderRequest`, `DriverStatus`, `TrafficInfo`).
2. **Real-time Pairwise Matching (Interval Join)**: Rider and Driver streams are key-partitioned by location and matched using `intervalJoin`,
   allowing for fine-grained many-to-many matching within a configurable time range.
3. **Event-Time Windowed Aggregation (Post-Join)**: The resulting `DemandSupply` records are grouped by location using a
   `TumblingEventTimeWindow` to compute demand/supply counts over fixed intervals,
   effectively compressing high-volume edge matches into summarized metrics at the cloud layer.
4. **Broadcast Traffic Info**: Real-time `TrafficInfo` events are broadcast to all downstream subtasks,
   and joined with the aggregated `DemandSupply` stream using broadcast state enrichment.
5. **Price Calculation**: compute final price using the formula:

   ```
   price = basePrice * (demand / supply) * nightFactor * trafficFactor
   ```

6. **Sink**: emit computed pricing results as JSON strings to Kafka topic `dynamic_price`.

---

## Project Structure

| File/Class                                                                     | Role                                                            |
|--------------------------------------------------------------------------------|-----------------------------------------------------------------|
| `DynamicPricingJob.java`                                                       | Main pipeline logic                                             |
| `RiderRequest.java`, `DriverStatus.java`, `TrafficInfo.java`                   | Data models                                                     |
| `RiderRequestParser.java`, `DriverStatusParser.java`, `TrafficInfoParser.java` | JSON deserialization                                            |
| `DemandSupply.java`                                                            | Aggregated supply/demand per location window                    |
| `EnrichedPricingInput.java`                                                    | Combines demand/supply and traffic info                         |
| `DemandSupplyEnricher.java`                                                    | Enriches `DemandSupply` with broadcast traffic data             |
| `DynamicPriceCalculator.java`                                                  | (Optional) Modular price calculation as `MapFunction`           |
| `PriceResult.java`                                                             | Final result object (not always used if emitting JSON directly) |

---

## Kafka Topics

| Topic            | Description                                      |
|------------------|--------------------------------------------------|
| `rider_requests` | Raw ride requests from riders (produced at edge) |
| `driver_status`  | Real-time driver availability info               |
| `traffic_info`   | Regional congestion levels (1 to 5)              |
| `dynamic_price`  | Output topic with JSON pricing results           |

---

## Deployment Notes

- The pipeline uses **event-time windows** with 5-minute tumbling aggregation.
- Traffic enrichment is done with **Flink Broadcast State**.
- To test the system, three mock Kafka producers should be launched separately to simulate real-world streaming data.

---

## Pipeline Diagram (Simplified)

```text
                  +--------------------+    
                  |  rider_requests    |--+     
                  +--------------------+  |     
                                           |         
                  +--------------------+   |        +-------------------------+
                  |  driver_status     |--+-->JOIN--> DemandSupply (windowed) +
                  +--------------------+            +-------------------------+
                                                     |
              +----------------------+               v
              |  traffic_info (BROADCAST)----------> Enricher (traffic factor)
              +----------------------+               |
                                                     v
                                               Price Calculation
                                                     |
                                                     v
                                           +--------------------+
                                           |   dynamic_price     |
                                           +--------------------+
```

---

## License
Apache 2.0

---

## Acknowledgements
This project is inspired by Uber's dynamic pricing strategies and is built for research, simulation, and performance benchmarking purposes in edge-to-cloud streaming scenarios.

