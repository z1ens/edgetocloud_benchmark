package dynamicpricing;

public class DriverStatus {
    private String driverId;
    private String location;
    private boolean isAvailable;
    private long timestamp;

    public DriverStatus(String driverId, String location, boolean isAvailable, long timestamp) {
        this.driverId = driverId;
        this.location = location;
        this.isAvailable = isAvailable;
        this.timestamp = timestamp;
    }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
