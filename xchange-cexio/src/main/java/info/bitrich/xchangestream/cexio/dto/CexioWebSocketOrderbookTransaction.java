package info.bitrich.xchangestream.cexio.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class CexioWebSocketOrderbookTransaction {
    
    protected static final String PROP_ID = "id";
    protected static final String PROP_PAIR = "pair";
    protected static final String PROP_BIDS = "bids";
    protected static final String PROP_ASKS = "asks";
    
    protected long id;
    protected String pair;
    protected BigDecimal[][] bids;
    protected BigDecimal[][] asks;

    public CexioWebSocketOrderbookTransaction(@JsonProperty(PROP_ID) long id, 
            @JsonProperty(PROP_PAIR) String pair, @JsonProperty(PROP_BIDS) BigDecimal[][] bids, 
            @JsonProperty(PROP_ASKS) BigDecimal[][] asks) {
        this.id = id;
        this.pair = pair;
        this.bids = bids;
        this.asks = asks;
    }
    
    public long getId() {
        return id;
    }
    
    public String getPair() {
        return pair;
    }
    
    public BigDecimal[][] getBids() {
        return bids;
    }
    
    public BigDecimal[][] getAsks() {
        return asks;
    }
    
    @JsonIgnore
    public abstract Date getTimestampAsDate();
    
    public abstract CexioOrderbook toCexioOrderBook(CexioOrderbook orderbook);

}
