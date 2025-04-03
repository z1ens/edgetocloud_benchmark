from flask import Flask, request, jsonify
import random

app = Flask(__name__)

@app.route('/predict', methods=['POST'])
def predict():
    data = request.json
    amount = data.get("amount", 0)

    # Simple rule: higher amount → higher fraud score
    score = min(1.0, max(0.0, (amount - 1000) / 4000))  # scaled from 0 to 1
    return jsonify({"score": round(score, 4)})

if __name__ == '__main__':
    app.run(port=5002)
