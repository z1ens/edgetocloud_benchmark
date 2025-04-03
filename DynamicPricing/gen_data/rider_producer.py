import json
import time
import uuid
import random
from confluent_kafka import Producer

p = Producer({'bootstrap.servers': 'localhost:9092'})
locations = ['district-1', 'district-2', 'district-3', 'district-4']

def generate_rider_request():
    rider = {
        'riderId': str(uuid.uuid4()),
        'location': random.choice(locations),
        'destination': random.choice(locations),
        'timestamp': int(time.time() * 1000)
    }

    # 5 % of abnormal data (no location)
    if random.random() < 0.05:
        rider['location'] = None

    return rider

def delivery_report(err, msg):
    if err is not None:
        print(f"Delivery failed: {err}")
    else:
        print(f"Produced message to {msg.topic()} [{msg.partition()}]")

while True:
    data = generate_rider_request()
    p.produce('rider_requests', json.dumps(data), callback=delivery_report)
    p.poll(0)
    time.sleep(0.2)  # to control the rate
