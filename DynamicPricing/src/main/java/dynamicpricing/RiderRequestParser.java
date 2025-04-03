package dynamicpricing;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RiderRequestParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static RiderRequest parse(String json) {
        try {
            return objectMapper.readValue(json, RiderRequest.class);
        } catch (Exception e) {
            System.err.println("Failed to parse JSON: " + json + ", error: " + e.getMessage());
            return null;
        }
    }
}
