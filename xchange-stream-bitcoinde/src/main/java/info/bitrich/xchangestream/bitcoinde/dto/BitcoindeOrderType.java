package info.bitrich.xchangestream.bitcoinde.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum BitcoindeOrderType {
  @JsonProperty("buy")
  BUY,
  @JsonProperty("sell")
  SELL;
}
