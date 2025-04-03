# Real-Time ATM Fraud Detection Pipeline (Apache Flink + Kafka)

This project implements a real-time streaming pipeline for detecting potential ATM fraud using Apache Flink and Apache Kafka. The pipeline simulates and analyzes ATM transactions and applies a layered processing approach that combines rule-based filtering, machine learning scoring, and pattern-based behavioral analysis.

---

## Pipeline Architecture

```
Kafka Source
   → Parse & Clean
   → Rule Filter (High Amount)
   → ML Model Scoring (via REST API)
   → Timestamp Assignment
   → KeyBy(Account)
       ├─ CEP Pattern Detection (High-Risk Sequences)
       └─ Windowed Aggregation (Frequent High Amounts)
           → Kafka Sink (Fraud Alerts)
```

---

## Key Features

### 1. **Data Source**
- Simulated ATM transaction data produced into Kafka topic `atm-transactions`
- Fields include: transactionId, accountId, timestamp, amount, location, atmId

### 2. **Stream Parsing and Cleansing**
- Parses raw CSV-like input using a custom `ATMTransactionParser`
- Filters out invalid/null entries

### 3. **Rule-Based Filtering**
- Transactions with amount > 1000.0 are flagged for further analysis

### 4. **Machine Learning Score Integration**
- Each high-amount transaction is enriched with a `fraudScore` by calling a local Python REST API
- The Python model is a placeholder; any scikit-learn / PyTorch model can be integrated easily

### 5. **Flink CEP Pattern Matching**
- Detects suspicious behavior: same account issues 2 consecutive high-risk transactions (fraudScore > 0.9) within 1 minute

### 6. **Windowed Aggregation**
- For each account, aggregates high-amount transactions in a tumbling event-time window (default 30s)
- Emits alerts when >= 3 such transactions occur

### 7. **Unified Kafka Sink**
- All alert outputs (from rules and CEP) are pushed to topic `atm-alerts`

---

## Components

| File | Purpose |
|------|---------|
| `ATMTransaction.java` | POJO for transaction data |
| `ATMTransactionParser.java` | Parses raw string into POJO |
| `AmountThresholdFilter.java` | Filters transactions over amount threshold |
| `FraudScoreEnrichment.java` | Calls external ML model for scoring |
| `FraudPatternDetector.java` | Defines and applies CEP pattern |
| `SuspiciousTransactionDetector.java` | Rule-based window aggregation |
| `ATMDetectionJob.java` | Main Flink job combining all components |
| `fraud_model_server.py` | Python REST API scoring service |

---

## How to Run

### Prerequisites
- Apache Flink 1.15+
- Kafka running locally (topic: `atm-transactions`, `atm-alerts`)
- Python 3 with Flask: `pip install flask`

### 1. Start ML Model Server
```bash
python fraud_model_server.py  # runs on http://localhost:5002/predict
```

### 2. Launch Flink Job
```bash
flink run -c atmdetection.ATMDetectionJob target/fraud-detection-0.1.jar \
  --input-topic atm-transactions \
  --output-topic atm-alerts \
  --bootstrap.servers localhost:9092
```

### 3. Produce Test Data
```bash
python generate_atm_data.py
```

---

## Possible Extensions
- Connect Flink sink to PostgreSQL or Elasticsearch for dashboards
- Swap out simple model with real fraud detection models (e.g., Random Forest, XGBoost)
- Add more CEP rules for diverse fraud behaviors
- Deploy on Kubernetes / Flink Cluster for benchmark testing
---
