package dynamicpricing;

public class DriverStatusParser {
    public static DriverStatus parse(String json) {
        try {
            String driverId = null;
            String location = null;
            boolean isAvailable = false;
            long timestamp = 0L;

            String[] parts = json.replaceAll("[{}\"]", "").split(",");
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length != 2) continue;
                switch (kv[0].trim()) {
                    case "driverId":
                        driverId = kv[1].trim();
                        break;
                    case "location":
                        location = kv[1].trim();
                        break;
                    case "isAvailable":
                        isAvailable = Boolean.parseBoolean(kv[1].trim());
                        break;
                    case "timestamp":
                        timestamp = Long.parseLong(kv[1].trim());
                        break;
                }
            }
            return new DriverStatus(driverId, location, isAvailable, timestamp);
        } catch (Exception e) {
            System.err.println("Failed to parse driver JSON manually: " + json + ", error: " + e.getMessage());
            return null;
        }
    }
}