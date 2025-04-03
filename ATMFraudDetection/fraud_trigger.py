from flask import Flask, request, jsonify
from kafka import KafkaProducer
import time
import random

app = Flask(__name__)

producer = KafkaProducer(
    bootstrap_servers='localhost:9092',
    value_serializer=lambda v: v.encode('utf-8')
)

def send_transaction(account_id, amount, location, atm_id):
    txn_id = f"TXN{int(time.time() * 1000)}{random.randint(100,999)}"
    timestamp = int(time.time() * 1000)
    line = f"{txn_id},{account_id},{timestamp},{amount},{location},{atm_id}"
    producer.send("atm-transactions", value=line)
    return line

@app.route('/trigger', methods=['POST'])
def trigger():
    data = request.json
    account_id = data.get("accountId", "ACC9999")
    amount = float(data.get("amount", 9999))
    location = data.get("location", "Unknown")
    atm_id = data.get("atmId", "ATM-999")

    sent = send_transaction(account_id, amount, location, atm_id)
    return jsonify({"status": "sent", "data": sent})

if __name__ == '__main__':
    app.run(port=5001)
