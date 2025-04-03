
import requests
import time
import pandas as pd

FLINK_REST_URL = "http://localhost:8081"

# get running job ID
def get_job_id():
    response = requests.get(f"{FLINK_REST_URL}/jobs").json()
    jobs = response.get("jobs", [])
    for job in jobs:
        if job["status"] == "RUNNING":
            return job["id"]
    return None

# get Operator ID
def get_operators(job_id):
    url = f"{FLINK_REST_URL}/jobs/{job_id}/plan"
    response = requests.get(url).json()
    return [(node["id"], node["description"]) for node in response["plan"]["nodes"]]

# calculate subtasks numbers
def get_parallelism(job_id, operator_id):
    url = f"{FLINK_REST_URL}/jobs/{job_id}/vertices/{operator_id}"
    response = requests.get(url).json()
    return response.get("parallelism", 1)

# get subtasks metrics, do some calculation
def get_metrics(job_id, operator_id, metric_name, aggregation="sum"):
    parallelism = get_parallelism(job_id, operator_id)
    values = []

    for subtask_index in range(parallelism):
        full_metric_name = f"{subtask_index}.{metric_name}"
        url = f"{FLINK_REST_URL}/jobs/{job_id}/vertices/{operator_id}/metrics?get={full_metric_name}"
        response = requests.get(url).json()
        if isinstance(response, list) and response:
            try:
                values.append(float(response[0]["value"]))
            except (ValueError, TypeError):
                continue

    if not values:
        return None

    if aggregation == "sum":
        return sum(values)
    elif aggregation == "avg":
        return sum(values) / len(values)
    elif aggregation == "max":
        return max(values)
    else:
        return values  # raw list


def collect_operator_metrics(job_id, operator_id):
    metrics = {
        "numRecordsInPerSecond": ("sum", None),
        "numRecordsOutPerSecond": ("sum", None),
        "busyTimeMsPerSecond": ("avg", None),
        "idleTimeMsPerSecond": ("avg", None),
        "mailboxLatencyMs_mean": ("avg", None),
        "backPressuredTimeMsPerSecond": ("avg", None),
    }

    for name in metrics:
        agg, _ = metrics[name]
        metrics[name] = (agg, get_metrics(job_id, operator_id, name, aggregation=agg))

    return metrics

# SOP
if __name__ == "__main__":
    job_id = get_job_id()
    if not job_id:
        print("No running Flink job")
        exit()

    operators = get_operators(job_id)
    print(f"Collecting Job {job_id} Operator metrics...\n")

    all_data = []
    for operator_id, description in operators:
        metrics = collect_operator_metrics(job_id, operator_id)
        metrics_row = {
            
            "description": description
        }
        for metric_name, (agg, value) in metrics.items():
            metrics_row[f"{metric_name} ({agg})"] = value

        all_data.append(metrics_row)

    df = pd.DataFrame(all_data)
    print(df.to_string(index=False))
