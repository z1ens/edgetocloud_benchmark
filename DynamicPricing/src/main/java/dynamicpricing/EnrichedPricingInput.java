package dynamicpricing;

public class EnrichedPricingInput {
    private DemandSupply demandSupply;
    private double trafficFactor;

    public EnrichedPricingInput() {}

    public EnrichedPricingInput(DemandSupply ds, double trafficFactor) {
        this.demandSupply = ds;
        this.trafficFactor = trafficFactor;
    }

    public DemandSupply getDemandSupply() { return demandSupply; }
    public void setDemandSupply(DemandSupply demandSupply) { this.demandSupply = demandSupply; }

    public double getTrafficFactor() { return trafficFactor; }
    public void setTrafficFactor(double trafficFactor) { this.trafficFactor = trafficFactor; }
}
