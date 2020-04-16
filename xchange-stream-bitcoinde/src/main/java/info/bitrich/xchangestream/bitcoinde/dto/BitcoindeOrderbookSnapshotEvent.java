package info.bitrich.xchangestream.bitcoinde.dto;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;

public class BitcoindeOrderbookSnapshotEvent extends BitcoindeEventMessage<LimitOrder>
    implements BitcoindeOrderbookEvent {

  public BitcoindeOrderbookSnapshotEvent(String name, LimitOrder[] args) {
    super(name, args);
  }

  @Override
  public BitcoindeOrderbook toBitcoindeOrderbook(
      CurrencyPair currencyPair, BitcoindeOrderbook orderbook) {
    return new BitcoindeOrderbook(getArgs());
  }
}
