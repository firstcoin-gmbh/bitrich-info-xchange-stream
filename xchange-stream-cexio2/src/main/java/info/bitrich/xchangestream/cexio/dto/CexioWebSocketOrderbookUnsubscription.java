package info.bitrich.xchangestream.cexio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class CexioWebSocketOrderbookUnsubscription extends CexioWebSocketPairSubscription {

  public CexioWebSocketOrderbookUnsubscription(@JsonProperty(PROP_PAIR) String[] pair) {
    super(pair);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("CexioWebSocketOrderbookUnsubscription [getPair()=");
    builder.append(Arrays.toString(getPair()));
    builder.append("]");
    return builder.toString();
  }
}
