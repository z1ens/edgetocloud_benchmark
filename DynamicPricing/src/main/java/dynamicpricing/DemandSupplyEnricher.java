package dynamicpricing;

import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

public class DemandSupplyEnricher extends BroadcastProcessFunction<DemandSupply, TrafficInfo, EnrichedPricingInput> {

    private final MapStateDescriptor<String, Integer> trafficStateDesc;

    public DemandSupplyEnricher(MapStateDescriptor<String, Integer> trafficStateDesc) {
        this.trafficStateDesc = trafficStateDesc;
    }

    @Override
    public void processElement(DemandSupply ds, ReadOnlyContext ctx, Collector<EnrichedPricingInput> out) throws Exception {
        Integer congestion = ctx.getBroadcastState(trafficStateDesc).get(ds.getLocation());
        double trafficFactor = 1.0;

        if (congestion != null) {
            trafficFactor = 1.0 + (congestion - 1) * 0.2; // 1: 1.0, 5: 1.8
        }

        out.collect(new EnrichedPricingInput(ds, trafficFactor));
    }

    @Override
    public void processBroadcastElement(TrafficInfo traffic, Context ctx, Collector<EnrichedPricingInput> out) throws Exception {
        ctx.getBroadcastState(trafficStateDesc).put(traffic.getLocation(), traffic.getCongestionLevel());
    }
}
