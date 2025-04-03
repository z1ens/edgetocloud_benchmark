from kafka import KafkaProducer
import threading
import time
import uuid
import random
import json

BOOTSTRAP_SERVERS = 'localhost:9092'
LOCATIONS = ['district-1', 'district-2', 'district-3', 'district-4']

producer = KafkaProducer(
    bootstrap_servers=BOOTSTRAP_SERVERS,
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

def produce_rider_requests(rate_per_sec=50):
    while True:
        for _ in range(rate_per_sec):
            data = {
                "riderId": str(uuid.uuid4()),
                "location": random.choice(LOCATIONS),
                "destination": random.choice(LOCATIONS),
                "timestamp": int(time.time() * 1000)
            }
            if random.random() < 0.05:
                data["location"] = None  # mimic bad data
            producer.send("rider-requests", data)
        time.sleep(1)

def produce_driver_status(rate_per_sec=30):
    while True:
        for _ in range(rate_per_sec):
            data = {
                "driverId": str(uuid.uuid4()),
                "location": random.choice(LOCATIONS),
                "isAvailable": random.random() > 0.1,
                "timestamp": int(time.time() * 1000)
            }
            producer.send("driver-status", data)
        time.sleep(1)

def produce_traffic_info(rate_per_sec=10):
    while True:
        for _ in range(rate_per_sec):
            data = {
                "location": random.choice(LOCATIONS),
                "congestionLevel": random.randint(1, 5),
                "timestamp": int(time.time() * 1000)
            }
            producer.send("traffic-info", data)
        time.sleep(1)

if __name__ == "__main__":
    print("Starting baseline data producers using kafka-python...")

    threading.Thread(target=produce_rider_requests, daemon=True).start()
    threading.Thread(target=produce_driver_status, daemon=True).start()
    threading.Thread(target=produce_traffic_info, daemon=True).start()

    try:
        while True:
            time.sleep(10)
    except KeyboardInterrupt:
        print("Stopped.")
