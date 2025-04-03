package dynamicpricing;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DriverStatusParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static DriverStatus parse(String json) {
        try {
            return objectMapper.readValue(json, DriverStatus.class);
        } catch (Exception e) {
            System.err.println("Failed to parse driver JSON: " + json + ", error: " + e.getMessage());
            return null;
        }
    }
}
