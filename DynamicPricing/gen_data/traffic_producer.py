# traffic_producer.py
from confluent_kafka import Producer
import time, json, random

p = Producer({'bootstrap.servers': 'localhost:9092'})
locations = ['district-1', 'district-2', 'district-3', 'district-4']

def generate_traffic():
    return {
        'location': random.choice(locations),
        'congestionLevel': random.randint(1, 5),  # 1 = low, 5 = heavy
        'timestamp': int(time.time() * 1000)
    }

while True:
    data = generate_traffic()
    p.produce('traffic_info', json.dumps(data))
    p.poll(0)
    time.sleep(5)  # slow down the updating rate of each time to sleep
