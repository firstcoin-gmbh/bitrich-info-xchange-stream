package info.bitrich.xchangestream.bitcoinde.dto;

import static info.bitrich.xchangestream.bitcoinde.BitcoindeAdapters.adaptOrderType;
import static info.bitrich.xchangestream.bitcoinde.BitcoindeAdapters.createOrder;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.stream.Stream;
import org.knowm.xchange.currency.CurrencyPair;

public class BitcoindeOrderAddedEvent extends BitcoindeEventMessage<BitcoindeOrderAdded>
    implements BitcoindeOrderbookEvent {

  public BitcoindeOrderAddedEvent(
      @JsonProperty("name") String name, @JsonProperty("args") BitcoindeOrderAdded[] args) {
    super(name, args);
  }

  @Override
  public BitcoindeOrderbook toBitcoindeOrderbook(
      CurrencyPair currencyPair, BitcoindeOrderbook orderbook) {
    Stream.of(getArgs())
        .forEach(
            order ->
                orderbook.addOrder(
                    order.orderId,
                    createOrder(currencyPair, order, adaptOrderType(order.orderType), null)));

    return orderbook;
  }
}
