package dynamicpricing;

public class TrafficInfoParser {
    public static TrafficInfo parse(String json) {
        try {
            String location = null;
            int congestionLevel = 1;
            long timestamp = 0L;

            String[] parts = json.replaceAll("[{}\"]", "").split(",");
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length != 2) continue;
                switch (kv[0].trim()) {
                    case "location":
                        location = kv[1].trim();
                        break;
                    case "congestionLevel":
                        congestionLevel = Integer.parseInt(kv[1].trim());
                        break;
                    case "timestamp":
                        timestamp = Long.parseLong(kv[1].trim());
                        break;
                }
            }
            return new TrafficInfo(location, congestionLevel, timestamp);
        } catch (Exception e) {
            System.err.println("Traffic parse error (manual): " + json + ", error: " + e.getMessage());
            return null;
        }
    }
}