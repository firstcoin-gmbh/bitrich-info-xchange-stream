package info.bitrich.xchangestream.cexio.dto;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CexioWebSocketOrderbookSubscription extends CexioWebSocketPairSubscription {
    
    private static final String PROP_SUBSCRIBE = "subscribe";
    private static final String PROP_DEPTH = "depth";

    private final boolean subscribe;
    private final int depth;
    
    public CexioWebSocketOrderbookSubscription(@JsonProperty(PROP_PAIR) String[] pair) {
        this(pair, true, 0);
    }
    
    public CexioWebSocketOrderbookSubscription(@JsonProperty(PROP_PAIR) String[] pair, 
            @JsonProperty(PROP_DEPTH) int depth) {
        this(pair, true, depth);
    }
    
    public CexioWebSocketOrderbookSubscription(@JsonProperty(PROP_PAIR) String[] pair, 
            @JsonProperty(PROP_SUBSCRIBE) boolean subscribe, @JsonProperty(PROP_DEPTH) int depth) {
        super(pair);
        this.subscribe = subscribe;
        this.depth = depth;
    }
    
    public boolean isSubscribe() {
        return subscribe;
    }
    
    public int getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CexioWebSocketOrderbookSubscription [pair=");
        builder.append(Arrays.toString(getPair()));
        builder.append(", subscribe=");
        builder.append(subscribe);
        builder.append(", depth=");
        builder.append(depth);
        builder.append("]");
        return builder.toString();
    }

}
