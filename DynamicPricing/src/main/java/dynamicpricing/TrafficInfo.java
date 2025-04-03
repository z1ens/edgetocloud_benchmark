package dynamicpricing;

public class TrafficInfo {
    private String location;
    private int congestionLevel; // e.g. 1~5
    private long timestamp;

    public TrafficInfo(String location, int congestionLevel, long timestamp) {
        this.location = location;
        this.congestionLevel = congestionLevel;
        this.timestamp = timestamp;
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(int congestionLevel) { this.congestionLevel = congestionLevel; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
