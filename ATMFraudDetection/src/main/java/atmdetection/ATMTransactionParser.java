package atmdetection;

public class ATMTransactionParser {
    public static ATMTransaction parse(String line) {
        try {
            // transactionId,accountId,timestamp,amount,location,atmId
            String[] parts = line.split(",");
            if (parts.length < 6) return null;

            String transactionId = parts[0].trim();
            String accountId = parts[1].trim();
            Long timestamp = Long.parseLong(parts[2].trim());
            Double amount = Double.parseDouble(parts[3].trim());
            String location = parts[4].trim();
            String atmId = parts[5].trim();

            return new ATMTransaction(transactionId, accountId, timestamp, amount, location, atmId);
        } catch (Exception e) {
            // Skip invalid line
            return null;
        }
    }
}
