import random
import time
import json
from datetime import datetime
from kafka import KafkaProducer

producer = KafkaProducer(
    bootstrap_servers='localhost:9092',
    value_serializer=lambda v: v.encode('utf-8')
)

ACCOUNTS = [f"ACC{1000 + i}" for i in range(10)]
ATMS = [f"ATM-{i:03d}" for i in range(5)]
LOCATIONS = ["New York", "Los Angeles", "Chicago", "Houston", "Miami"]

def generate_transaction():
    transaction = {
        "transactionId": f"TXN{int(time.time() * 1000)}{random.randint(100,999)}",
        "accountId": random.choice(ACCOUNTS),
        "timestamp": int(time.time() * 1000),  # milliseconds since epoch
        "amount": round(random.uniform(20, 5000), 2),
        "location": random.choice(LOCATIONS),
        "atmId": random.choice(ATMS)
    }
    return transaction

if __name__ == '__main__':
    topic = "atm-transactions"
    print(f"Producing data to Kafka topic: {topic}")
    
    while True:
        txn = generate_transaction()
        csv_line = f"{txn['transactionId']},{txn['accountId']},{txn['timestamp']},{txn['amount']},{txn['location']},{txn['atmId']}"
        producer.send(topic, value=csv_line)
        print("Sent:", csv_line)
        time.sleep(random.uniform(0.2, 1.0))  # simulate variable transaction rate
