package info.bitrich.xchangestream.bitcoinde.dto;

import org.knowm.xchange.currency.CurrencyPair;

public interface BitcoindeOrderbookEvent {

  default BitcoindeOrderbook toBitcoindeOrderbook(
      CurrencyPair currencyPair, BitcoindeOrderbook orderbook) {
    return orderbook;
  }
}
