package info.bitrich.xchangestream.bitfinex.dto;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static info.bitrich.xchangestream.bitfinex.BitfinexUtils.toSymbolString;

import java.math.BigDecimal;

import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.utils.Assert;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BitfinexWebSocketNewOrder {

    private static final String PROP_GROUP_ID = "gid";
    private static final String PROP_CLIENT_ID = "cid";
    private static final String PROP_TYPE = "type";
    private static final String PROP_SYMBOL = "symbol";
    private static final String PROP_AMOUNT = "amount";
    private static final String PROP_PRICE = "price";
    
    @JsonProperty(PROP_GROUP_ID)
    @JsonInclude(NON_NULL)
    private String groupId;
    
    @JsonProperty(PROP_CLIENT_ID)
    @JsonInclude(NON_NULL)
    private String clientId;
    
    @JsonProperty(PROP_TYPE)
    private String type;
    
    @JsonProperty(PROP_SYMBOL)
    private String symbol;
    
    @JsonProperty(PROP_AMOUNT)
    private String amount;
    
    @JsonProperty(PROP_PRICE)
    private String price;
    
    public BitfinexWebSocketNewOrder(LimitOrder limitOrder) {
        Assert.notNull(limitOrder.getCurrencyPair(), "currency pair required!");
        Assert.notNull(limitOrder.getType(), "order type required!");
        Assert.notNull(limitOrder.getOriginalAmount(), "original amount required!");
        Assert.notNull(limitOrder.getLimitPrice(), "limit price required!");
        this.type = "EXCHANGE LIMIT"; 
        this.symbol = toSymbolString(limitOrder.getCurrencyPair());
        final OrderType orderType = limitOrder.getType();
        final BigDecimal originalAmount = limitOrder.getOriginalAmount().abs();
        if (orderType == OrderType.BID) {
            this.amount = originalAmount.toPlainString();
        } else if (orderType == OrderType.ASK) {
            this.amount = originalAmount.negate().toPlainString();
        } else {
            throw new IllegalArgumentException(String.format("Illegal order type %s. Use %s or %s instead!!!", 
                    orderType, OrderType.BID, OrderType.ASK));
        }
        final BigDecimal limitPrice = limitOrder.getLimitPrice().abs();
        this.price = limitPrice.toPlainString();
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public BitfinexWebSocketNewOrder setGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public BitfinexWebSocketNewOrder setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }
    
    public String getType() {
        return type;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public String getAmount() {
        return amount;
    }
    
    public String getPrice() {
        return price;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BitfinexWebSocketNewOrder [groupId=");
        builder.append(groupId);
        builder.append(", clientId=");
        builder.append(clientId);
        builder.append(", type=");
        builder.append(type);
        builder.append(", symbol=");
        builder.append(symbol);
        builder.append(", amount=");
        builder.append(amount);
        builder.append(", price=");
        builder.append(price);
        builder.append("]");
        return builder.toString();
    }
    
}
