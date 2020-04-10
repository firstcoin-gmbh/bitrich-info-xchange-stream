package info.bitrich.xchangestream.bitcoinde.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BitcoindeOrderTransaction {

  @JsonProperty("id")
  public long id;

  @JsonProperty("type")
  public String type;

  @JsonProperty("order_id")
  public String orderId;

  @JsonProperty("order_type")
  public BitcoindeOrderType orderType;

  @JsonProperty("trading_pair")
  public String tradingPair;
}
