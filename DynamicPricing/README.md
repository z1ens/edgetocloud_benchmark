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

1. **Parse & Clean** incoming JSON events into POJOs (`RiderRequest`, `DriverStatus`, `TrafficInfo`) using lightweight custom parsers. After parsing, the records pass through stateless map and filter operators to filter out malformed or unavailable events and annotate records with event time. 
2. **Real-time Pairwise Matching (Interval Join)**: The rider and driver streams are keyed by location and joined using Flink's `intervalJoin` operator over a ±30-second window. This enables fine-grained matching between riders and available drivers within the same region and a realistic time tolerance.
3. **Event-Time Windowed Aggregation (Post-Join)**: The resulting `DemandSupply` records are grouped by location using a
   `TumblingEventTimeWindow` to compute demand/supply counts over fixed intervals,
   effectively compressing high-volume edge matches into summarized metrics at the cloud layer.
4. **Broadcast Traffic Info**: The output of the interval join is fed into a `TumblingEventTimeWindow (10s)`, where matched (location, demand, supply) records are aggregated: grouped by location
and reduced to count total rider/driver matches per window.
   and joined with the aggregated `DemandSupply` stream using broadcast state enrichment.
5. **Price Calculation**: Each enriched record is processed to compute a final surge price based on:

   ```
   price = basePrice * (demand / supply) * nightFactor * trafficFactor
   ```

6. **Sink**: The result is serialized into JSON and sent to a Kafka sink for downstream processing or monitoring.

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
