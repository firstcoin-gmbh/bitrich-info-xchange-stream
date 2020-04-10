package info.bitrich.xchangestream.cexio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class CexioWebSocketPairSubscription {

  protected static final String PROP_PAIR = "pair";

  private final String[] pair;

  public CexioWebSocketPairSubscription(@JsonProperty(PROP_PAIR) String[] pair) {
    this.pair = pair;
  }

  public String[] getPair() {
    return pair;
  }
}
