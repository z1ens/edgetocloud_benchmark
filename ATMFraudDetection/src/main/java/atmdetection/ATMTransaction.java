package atmdetection;

public class ATMTransaction {
    private String transactionId;
    private String accountId;
    private Long timestamp;
    private Double amount;
    private String location;
    private String atmId;
    private Double fraudScore; // use a CEP based or other event model to score each fraud attempt
    private transient long processingStartTime;

    public ATMTransaction(String transactionId, String accountId, Long timestamp, Double amount, String location, String atmId) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.timestamp = timestamp;
        this.amount = amount;
        this.location = location;
        this.atmId = atmId;
        this.processingStartTime = System.nanoTime();
    }

    public String getTransactionId() { return transactionId; }
    public String getAccountId() { return accountId; }
    public Long getTimestamp() { return timestamp; }
    public Double getAmount() { return amount; }
    public String getLocation() { return location; }
    public String getAtmId() { return atmId; }
    public Double getFraudScore() { return fraudScore; }
    public long getProcessingStartTime() { return processingStartTime; }

    public void setFraudScore(Double fraudScore) { this.fraudScore = fraudScore; }
    public void setProcessingStartTime(long processingStartTime) { this.processingStartTime = processingStartTime; }

    @Override
    public String toString() {
        return "ATMTransaction{" +
                "transactionId='" + transactionId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", timestamp=" + timestamp +
                ", amount=" + amount +
                ", location='" + location + '\'' +
                ", atmId='" + atmId + '\'' +
                ", fraudScore=" + fraudScore +
                '}';
    }
}
