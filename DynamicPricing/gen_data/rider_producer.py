import json
import time
import uuid
import random
from kafka import KafkaProducer

producer = KafkaProducer(
    bootstrap_servers='localhost:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

locations = ['district-1', 'district-2', 'district-3', 'district-4']

def generate_rider_request():
    rider = {
        'riderId': str(uuid.uuid4()),
        'location': random.choice(locations),
        'destination': random.choice(locations),
        'timestamp': int(time.time() * 1000)
    }

    # 5% abnormal data (no location)
    if random.random() < 0.05:
        rider['location'] = None

    return rider

def delivery_report(record_metadata, exception=None):
    if exception is not None:
        print(f"Delivery failed: {exception}")
    else:
        print(f"Produced message to {record_metadata.topic} [{record_metadata.partition}]")

while True:
    data = generate_rider_request()
    future = producer.send('rider_requests', value=data)
    future.add_callback(lambda record_metadata: delivery_report(record_metadata))
    future.add_errback(lambda exc: delivery_report(None, exc))
    producer.flush()
    time.sleep(0.2)
