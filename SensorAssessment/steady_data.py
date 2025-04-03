import time
import random
import argparse
from kafka import KafkaProducer

def generate_sensor_csv():
    timestamp = int(time.time() * 1e6)
    epoch = random.randint(1, 1000)
    moteId = random.randint(1, 10)
    temperature = round(random.uniform(20, 80), 4)
    humidity = round(random.uniform(30, 70), 4)
    light = round(random.uniform(100, 1000), 4)
    voltage = round(random.uniform(2, 3), 4)
    return f"{timestamp},{epoch},{moteId},{temperature},{humidity},{light},{voltage}"

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bootstrap_servers", default="localhost:9092")
    parser.add_argument("--topic", default="sensor-data")
    parser.add_argument("--rate", type=int, default=1000, help="Messages per second")
    args = parser.parse_args()

    producer = KafkaProducer(
        bootstrap_servers=args.bootstrap_servers,
        value_serializer=lambda v: v.encode("utf-8")
    )

    interval = 1.0 / args.rate
    print(f"Sending data at {args.rate} msg/s...")

    try:
        while True:
            start = time.time()
            for _ in range(args.rate):
                msg = generate_sensor_csv()
                producer.send(args.topic, msg)
            producer.flush()
            elapsed = time.time() - start
            sleep_time = max(0, 1.0 - elapsed)
            time.sleep(sleep_time)
    except KeyboardInterrupt:
        print("\nStopped by user.")
    finally:
        producer.close()

if __name__ == "__main__":
    main()
