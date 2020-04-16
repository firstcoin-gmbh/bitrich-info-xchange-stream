package info.bitrich.xchangestream.bitcoinde.dto;

import static info.bitrich.xchangestream.bitcoinde.BitcoindeAdapters.adaptOrderType;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.stream.Stream;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.exceptions.ExchangeException;

public class BitcoindeOrderRemovedEvent extends BitcoindeEventMessage<BitcoindeOrderRemoved>
    implements BitcoindeOrderbookEvent {

  public BitcoindeOrderRemovedEvent(
      @JsonProperty("name") String name, @JsonProperty("args") BitcoindeOrderRemoved[] args) {
    super(name, args);
  }

  @Override
  public BitcoindeOrderbook toBitcoindeOrderbook(
      CurrencyPair currencyPair, BitcoindeOrderbook orderbook) {
    final boolean[] removed = new boolean[] {false};

    Stream.of(getArgs())
        .forEach(
            order ->
                removed[0] =
                    orderbook.removeOrder(order.orderId, adaptOrderType(order.orderType))
                        || removed[0]);

    if (!removed[0]) {
      throw new ExchangeException(String.format("Nothing to remove in orderbook %s", currencyPair));
    }

    return orderbook;
  }
}
