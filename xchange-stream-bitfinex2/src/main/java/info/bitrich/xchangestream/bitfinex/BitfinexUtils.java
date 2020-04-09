package info.bitrich.xchangestream.bitfinex;

import static java.math.BigDecimal.ZERO;
import static org.knowm.xchange.bitfinex.v1.BitfinexUtils.adaptXchangeCurrency;

import com.fasterxml.jackson.databind.JsonNode;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthOrder;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthTrade;
import java.math.BigDecimal;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexOrderStatusResponse;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexTradeResponse;
import org.knowm.xchange.currency.CurrencyPair;

public class BitfinexUtils {

  public static final Number AUTH_CHANNEL_ID = 0;

  private static final String SYMBOL_PREFIX = "t";
  private static final String STATUS_CANCELED = "CANCELED".toLowerCase();
  private static final String STATUS_FILLED = "EXECUTED".toLowerCase();

  private BitfinexUtils() {}

  public static String adaptSymbol(String bitfinexSymbol) {
    if (bitfinexSymbol.startsWith(SYMBOL_PREFIX)) {
      return bitfinexSymbol.substring(1);
    }

    return bitfinexSymbol;
  }

  public static BitfinexTradeResponse adaptTradeResponse(BitfinexWebSocketAuthTrade trade) {
    String type = null;

    if (trade.getExecAmount().compareTo(BigDecimal.ZERO) < 0) {
      type = "sell";
    } else {
      type = "buy";
    }

    return new BitfinexTradeResponse(
        trade.getExecPrice(),
        trade.getExecAmount().abs(),
        new BigDecimal(trade.getMtsCreate() / 1000),
        "bitfinex",
        type,
        String.valueOf(trade.getId()),
        String.valueOf(trade.getOrderId()),
        trade.getFee(),
        trade.getFeeCurrency());
  }

  public static BitfinexOrderStatusResponse adaptOrderStatusResponse(
      BitfinexWebSocketAuthOrder order) {
    final String status =
        order.getOrderStatus() != null ? order.getOrderStatus().toLowerCase() : "";
    boolean isCancelled = status.contains(STATUS_CANCELED);
    boolean isLive = !isCancelled && !status.contains(STATUS_FILLED);
    boolean wasForced = false;
    final BigDecimal originalAmount = order.getAmountOrig().abs();
    final BigDecimal remainingAmount = order.getAmount().abs();
    final BigDecimal executedAmount = originalAmount.subtract(remainingAmount);
    String side = null;

    if (order.getAmountOrig().compareTo(ZERO) < 0) {
      side = "sell";
    } else {
      side = "buy";
    }

    return new BitfinexOrderStatusResponse(
        order.getId(),
        adaptSymbol(order.getSymbol()),
        order.getPrice(),
        order.getPriceAvg(),
        side,
        order.getType().toLowerCase(),
        new BigDecimal(order.getMtsCreate() / 1000),
        isLive,
        isCancelled,
        wasForced,
        originalAmount,
        remainingAmount,
        executedAmount);
  }

  public static String toSymbolString(CurrencyPair currencyPair) {
    return SYMBOL_PREFIX
        + adaptXchangeCurrency(currencyPair.base).toUpperCase()
        + adaptXchangeCurrency(currencyPair.counter).toUpperCase();
  }

  public static boolean hasMoreThanOneUpdateMessage(JsonNode rootNode) {
    final JsonNode messagesArray = rootNode.get(2);

    return messagesArray.has(0) && messagesArray.get(0).isArray();
  }

  public static boolean hasOnlyOneUpdateMessage(JsonNode rootNode) {
    final JsonNode messagesArray = rootNode.get(2);

    return messagesArray.has(0) && !messagesArray.get(0).isArray();
  }
}
