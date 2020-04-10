package info.bitrich.xchangestream.liquid.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"price", "amount"})
public class LiquidOrderbookLevel {

  private BigDecimal price;
  private BigDecimal amount;

  public LiquidOrderbookLevel(
      @JsonProperty("price") BigDecimal price, @JsonProperty("amount") BigDecimal amount) {
    this.price = price;
    this.amount = amount;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("PriceLevel [price=");
    builder.append(price);
    builder.append(", amount=");
    builder.append(amount);
    builder.append("]");
    return builder.toString();
  }
}
