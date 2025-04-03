package dynamicpricing;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TrafficInfoParser {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static TrafficInfo parse(String json) {
        try {
            return mapper.readValue(json, TrafficInfo.class);
        } catch (Exception e) {
            System.err.println("Traffic parse error: " + e.getMessage());
            return null;
        }
    }
}
