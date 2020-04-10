package info.bitrich.xchangestream.cexio.dto;

import static info.bitrich.xchangestream.cexio.CexioAdapters.COUNTER;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CexioWebSocketSubscriptionMessage {

  private final String e;
  private final CexioWebSocketPairSubscription data;
  private final String oid;

  public CexioWebSocketSubscriptionMessage(
      @JsonProperty("e") String e, @JsonProperty("data") CexioWebSocketPairSubscription data) {
    this(e, data, System.currentTimeMillis() + "_" + COUNTER.incrementAndGet() + "_" + e);
  }

  public CexioWebSocketSubscriptionMessage(
      @JsonProperty("e") String e,
      @JsonProperty("data") CexioWebSocketPairSubscription data,
      @JsonProperty("oid") String oid) {
    this.e = e;
    this.data = data;
    this.oid = oid;
  }

  public String getE() {
    return e;
  }

  public CexioWebSocketPairSubscription getData() {
    return data;
  }

  public String getOid() {
    return oid;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("CexioWebSocketSubscriptionMessage [e=");
    builder.append(e);
    builder.append(", data=");
    builder.append(data);
    builder.append(", oid=");
    builder.append(oid);
    builder.append("]");
    return builder.toString();
  }
}
