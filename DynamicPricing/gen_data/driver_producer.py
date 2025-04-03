from confluent_kafka import Producer
import uuid, time, random, json

p = Producer({'bootstrap.servers': 'localhost:9092'})
locations = ['district-1', 'district-2', 'district-3', 'district-4']

def generate_driver():
    return {
        'driverId': str(uuid.uuid4()),
        'location': random.choice(locations),
        'isAvailable': random.random() > 0.1,  # 90% available
        'timestamp': int(time.time() * 1000)
    }

while True:
    data = generate_driver()
    p.produce('driver_status', json.dumps(data))
    p.poll(0)
    time.sleep(0.3)
