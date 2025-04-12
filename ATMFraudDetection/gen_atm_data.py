import random
import time
from kafka import KafkaProducer

producer = KafkaProducer(
    bootstrap_servers='localhost:9092',
    value_serializer=lambda v: v.encode('utf-8')
)

# Account, city and also ATM
ACCOUNTS = [f"ACC{1000 + i}" for i in range(10)]
ATMS = [f"ATM-{i:03d}" for i in range(5)]
LOCATIONS = ["New York", "Los Angeles", "Chicago", "Houston", "Miami"]

# generate data for transactions
def generate_transaction():
    timestamp = int(time.time() * 1000)
    transaction_id = f"TXN{timestamp}{random.randint(100,999)}"
    account_id = random.choice(ACCOUNTS)
    amount = round(random.uniform(20, 5000), 2)
    location = random.choice(LOCATIONS)
    atm_id = random.choice(ATMS)
    return f"{transaction_id},{account_id},{timestamp},{amount},{location},{atm_id}"

if __name__ == '__main__':
    topic = "atm-transactions"
    rate = 1000  # 1000 messages per second

    print(f"Producing data to Kafka topic: {topic} at {rate} msgs/sec")

    try:
        while True:
            start = time.time()
            for _ in range(rate):
                record = generate_transaction()
                producer.send(topic, value=record)
            producer.flush()
            elapsed = time.time() - start
            time.sleep(max(0, 1.0 - elapsed))  # one time per second
    except KeyboardInterrupt:
        print("\nStopped by user.")
    finally:
        producer.close()
