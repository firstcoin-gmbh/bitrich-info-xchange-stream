package info.bitrich.xchangestream.bitfinex.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"type", "currency", "balance", "unsettledInterest", "balanceAvailable"})
public class BitfinexWebSocketWalletBalance {

  private String type;
  private String currency;
  private BigDecimal balance;
  private BigDecimal unsettledInterest;
  private BigDecimal balanceAvailable;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public BigDecimal getUnsettledInterest() {
    return unsettledInterest;
  }

  public void setUnsettledInterest(BigDecimal unsettledInterest) {
    this.unsettledInterest = unsettledInterest;
  }

  public BigDecimal getBalanceAvailable() {
    return balanceAvailable;
  }

  public void setBalanceAvailable(BigDecimal balanceAvailable) {
    this.balanceAvailable = balanceAvailable;
  }
}
