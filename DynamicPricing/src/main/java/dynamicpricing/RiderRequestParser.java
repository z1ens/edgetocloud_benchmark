package dynamicpricing;

public class RiderRequestParser {
    public static RiderRequest parse(String json) {
        try {
            String riderId = null;
            String location = null;
            String destination = null;
            long timestamp = 0L;

            String[] parts = json.replaceAll("[{}\"]", "").split(",");
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length != 2) continue;
                switch (kv[0].trim()) {
                    case "riderId":
                        riderId = kv[1].trim();
                        break;
                    case "location":
                        location = kv[1].trim();
                        break;
                    case "destination":
                        destination = kv[1].trim();
                        break;
                    case "timestamp":
                        timestamp = Long.parseLong(kv[1].trim());
                        break;
                }
            }
            return new RiderRequest(riderId, location, destination, timestamp);
        } catch (Exception e) {
            System.err.println("Failed to parse JSON manually: " + json + ", error: " + e.getMessage());
            return null;
        }
    }
}
