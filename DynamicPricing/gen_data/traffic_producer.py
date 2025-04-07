# traffic_producer.py
from kafka import KafkaProducer
import time, json, random

producer = KafkaProducer(
    bootstrap_servers='localhost:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

locations = ['district-1', 'district-2', 'district-3', 'district-4']

def generate_traffic():
    return {
        'location': random.choice(locations),
        'congestionLevel': random.randint(1, 5),  # 1 = low, 5 = heavy
        'timestamp': int(time.time() * 1000)
    }

while True:
    data = generate_traffic()
    producer.send('traffic_info', value=data)
    producer.flush()
    time.sleep(5)  # slowly update
