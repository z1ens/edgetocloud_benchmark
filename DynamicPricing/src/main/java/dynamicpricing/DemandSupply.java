package dynamicpricing;

public class DemandSupply {
    private String location;
    private long windowStart;
    private long windowEnd;
    private long demandCount;
    private long supplyCount;

    public DemandSupply(String location, long windowStart, long windowEnd, long demand, long supply) {
        this.location = location;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.demandCount = demand;
        this.supplyCount = supply;
    }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public long getWindowStart() { return windowStart; }
    public void setWindowStart(long windowStart) { this.windowStart = windowStart; }
    public long getWindowEnd() { return windowEnd; }
    public void setWindowEnd(long windowEnd) { this.windowEnd = windowEnd; }
    public long getDemandCount() { return demandCount; }
    public void setDemandCount(long demandCount) { this.demandCount = demandCount; }
    public long getSupplyCount() { return supplyCount; }
}
