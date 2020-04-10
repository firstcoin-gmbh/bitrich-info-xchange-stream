package info.bitrich.xchangestream.bitcoinde.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitcoindeOrderRemoved extends BitcoindeOrderTransaction {

  @JsonProperty("reason")
  public String reason;

  @JsonProperty("trade_user_id")
  public String tradeUserId;

  @JsonProperty("amount")
  public BigDecimal amount;

  @JsonProperty("price")
  public BigDecimal price;

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder
        .append("BitcoindeOrderRemoved [")
        .append("id=")
        .append(id)
        .append(", type=")
        .append(type)
        .append(", orderId=")
        .append(orderId)
        .append(", orderType=")
        .append(orderType)
        .append(", tradingPair=")
        .append(tradingPair)
        .append(", tradeUserId=")
        .append(tradeUserId)
        .append(", reason=")
        .append(reason)
        .append(", price=")
        .append(price)
        .append(", amount=")
        .append(amount)
        .append("]");
    return builder.toString();
  }
}
