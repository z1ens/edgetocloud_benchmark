from kafka import KafkaProducer
import uuid, time, random, json

# initialise Kafka
producer = KafkaProducer(
    bootstrap_servers='localhost:9092',
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

locations = ['district-1', 'district-2', 'district-3', 'district-4']

def generate_driver():
    return {
        'driverId': str(uuid.uuid4()),
        'location': random.choice(locations),
        'isAvailable': random.random() > 0.1,  # 90% available
        'timestamp': int(time.time() * 1000)
    }

# continuous flow
while True:
    data = generate_driver()
    producer.send('driver_status', value=data)
    producer.flush()  # make sure its send
    time.sleep(0.3)
