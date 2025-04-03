import time
import random
import argparse
from kafka import KafkaProducer

# generate_sensor_csv
def generate_sensor_csv():
    timestamp = int(time.time() * 1e6)  # milliseconds
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
    parser.add_argument("--normal_rate", type=int, default=1000)
    parser.add_argument("--burst_rate", type=int, default=10000)  # burst
    parser.add_argument("--normal_duration", type=int, default=10)  # steady time
    parser.add_argument("--burst_duration", type=int, default=5)    # burst time
    parser.add_argument("--cycles", type=int, default=3)  # number of iterations

    args = parser.parse_args()

    producer = KafkaProducer(
        bootstrap_servers=args.bootstrap_servers,
        value_serializer=lambda v: v.encode("utf-8")
    )

    for cycle in range(args.cycles):
        print(f"▶️ Cycle {cycle+1} - Normal traffic: {args.normal_rate}/s")
        for _ in range(args.normal_duration):
            for _ in range(args.normal_rate):
                line = generate_sensor_csv()
                producer.send(args.topic, value=line)
            time.sleep(1)

        print(f"⚠️ Cycle {cycle+1} - BURST traffic: {args.burst_rate}/s")
        for _ in range(args.burst_duration):
            for _ in range(args.burst_rate):
                line = generate_sensor_csv()
                producer.send(args.topic, value=line)
            time.sleep(1)

    print("✅ All cycles complete.")
    producer.flush()
    producer.close()

if __name__ == "__main__":
    main()
