package info.bitrich.xchangestream.cexio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

public class CexioWebSocketOrderbookUpdate extends CexioWebSocketOrderbookTransaction {

  private static final String PROP_TIME = "time";

  private long time;

  public CexioWebSocketOrderbookUpdate(
      @JsonProperty(PROP_ID) long id,
      @JsonProperty(PROP_PAIR) String pair,
      @JsonProperty(PROP_BIDS) BigDecimal[][] bids,
      @JsonProperty(PROP_ASKS) BigDecimal[][] asks,
      @JsonProperty(PROP_TIME) long time) {
    super(id, pair, bids, asks);
    this.time = time;
  }

  public long getTime() {
    return time;
  }

  @Override
  public Date getTimestampAsDate() {
    return new Date(time);
  }

  @Override
  public CexioOrderbook toCexioOrderBook(CexioOrderbook orderbook) {
    if (orderbook != null) {
      orderbook.updateData(id, time / 1000L, getBids(), getAsks());
    }
    return orderbook;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("CexioWebSocketOrderbookUpdate [time=");
    builder.append(time);
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
