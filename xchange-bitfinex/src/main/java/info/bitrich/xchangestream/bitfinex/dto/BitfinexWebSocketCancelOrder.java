package info.bitrich.xchangestream.bitfinex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BitfinexWebSocketCancelOrder {
    
    private static final String PROP_ORDER_ID = "id";
    
    @JsonProperty(PROP_ORDER_ID)
    private Number orderId;

    public BitfinexWebSocketCancelOrder(Number orderId) {
        this.orderId = orderId;
    }
    
    public Number getOrderId() {
        return orderId;
    }

}
