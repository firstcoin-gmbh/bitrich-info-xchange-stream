package info.bitrich.xchangestream.bitcoinde.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BitcoindeOrderAdded extends BitcoindeOrderTransaction {

  @JsonProperty("amount")
  public BigDecimal amount;

  @JsonProperty("price")
  public BigDecimal price;

  @JsonProperty("volume")
  public BigDecimal volume;

  @JsonProperty("min_amount")
  public BigDecimal minAmount;

  @JsonProperty("payment_option")
  public int paymentOption;

  @JsonProperty("min_trust_level")
  public String minTrustLevel;

  @JsonProperty("only_kyc_full")
  public int onlyKycFull;

  @JsonProperty("trade_to_sepa_country")
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  public String[] tradeToSepaCountry;

  @JsonProperty("uid")
  public String userId;

  @JsonProperty("is_kyc_full")
  public int isKycFull;

  @JsonProperty("bic_full")
  public String bicFull;

  @JsonProperty("bic_short")
  public String bicShort;

  @JsonProperty("seat_of_bank_of_creator")
  public String seatOfBankOfCreator;

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder
        .append("BitcoindeOrderAdded [")
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
        .append(", amount=")
        .append(amount)
        .append(", price=")
        .append(price)
        .append(", volume=")
        .append(volume)
        .append(", minAmount=")
        .append(minAmount)
        .append(", paymentOption=")
        .append(paymentOption)
        .append(", minTrustLevel=")
        .append(minTrustLevel)
        .append(", onlyKycFull=")
        .append(onlyKycFull)
        .append(", tradeToSepaCountry=")
        .append(Arrays.toString(tradeToSepaCountry))
        .append(", userId=")
        .append(userId)
        .append(", isKycFull=")
        .append(isKycFull)
        .append(", bicFull=")
        .append(bicFull)
        .append(", bicShort=")
        .append(bicShort)
        .append(", seatOfBankOfCreator=")
        .append(seatOfBankOfCreator)
        .append("]");
    return builder.toString();
  }
}
