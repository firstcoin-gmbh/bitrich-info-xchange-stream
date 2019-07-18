package info.bitrich.xchangestream.cexio.dto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CexioWebSocketOrderbookSnapshot extends CexioWebSocketOrderbookTransaction {
    
    private static final String PROP_TIMESTAMP = "timestamp";
    private static final String PROP_SELL_TOTAL = "sell_total";
    private static final String PROP_BUY_TOTAL = "buy_total";
    
    private long timestamp;
    private BigDecimal sellTotal;
    private BigDecimal buyTotal;

    public CexioWebSocketOrderbookSnapshot(@JsonProperty(PROP_ID) long id, 
            @JsonProperty(PROP_PAIR) String pair, @JsonProperty(PROP_BIDS) BigDecimal[][] bids, 
            @JsonProperty(PROP_ASKS) BigDecimal[][] asks, @JsonProperty(PROP_TIMESTAMP) long timestamp, 
            @JsonProperty(PROP_SELL_TOTAL) BigDecimal sellTotal, 
            @JsonProperty(PROP_BUY_TOTAL) BigDecimal buyTotal) {
        super(id, pair, bids, asks);
        this.timestamp = timestamp;
        this.sellTotal = sellTotal;
        this.buyTotal = buyTotal;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public BigDecimal getSellTotal() {
        return sellTotal;
    }
    
    public BigDecimal getBuyTotal() {
        return buyTotal;
    }

    @Override
    public Date getTimestampAsDate() {
        return new Date(timestamp * 1000L);
    }

    @Override
    public CexioOrderbook toCexioOrderBook(CexioOrderbook orderbook) {
        return new CexioOrderbook(timestamp, getBids(), getAsks());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CexioWebSocketOrderbookSnapshot [timestamp=");
        builder.append(timestamp);
        builder.append(", sellTotal=");
        builder.append(sellTotal);
        builder.append(", buyTotal=");
        builder.append(buyTotal);
        builder.append(", getTimestampAsDate()=");
        builder.append(getTimestampAsDate());
        builder.append(", getId()=");
        builder.append(getId());
        builder.append(", getPair()=");
        builder.append(getPair());
        builder.append(", getBids()=");
        builder.append(Arrays.toString(getBids()));
        builder.append(", getAsks()=");
        builder.append(Arrays.toString(getAsks()));
        builder.append("]");
        return builder.toString();
    }

}
