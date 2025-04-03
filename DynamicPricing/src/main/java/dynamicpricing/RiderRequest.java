package dynamicpricing;

public class RiderRequest {
    private String riderId;
    private String location;       // starting location “district-1”
    private String destination;    // destination
    private long timestamp;        // time when requesting the ride

    public RiderRequest(String riderId, String location, String destination, long timestamp) {
        this.riderId = riderId;
        this.location = location;
        this.destination = destination;
        this.timestamp = timestamp;
    }

    public String getRiderId() {
        return riderId;
    }

    public void setRiderId(String riderId) {
        this.riderId = riderId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

