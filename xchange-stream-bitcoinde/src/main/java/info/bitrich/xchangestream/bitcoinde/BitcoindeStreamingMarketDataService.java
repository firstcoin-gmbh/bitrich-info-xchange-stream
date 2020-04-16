package info.bitrich.xchangestream.bitcoinde;

import static info.bitrich.xchangestream.bitcoinde.BitcoindeAdapters.*;

import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderbook;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderbookSnapshotEvent;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Observable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.knowm.xchange.bitcoinde.v4.service.OrderbookOrdersParams;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitcoindeStreamingMarketDataService implements StreamingMarketDataService {

  private static final Logger LOG =
      LoggerFactory.getLogger(BitcoindeStreamingMarketDataService.class);

  private final Map<CurrencyPair, BitcoindeOrderbook> orderbooks = new HashMap<>();
  private final BitcoindeStreamingService streamingService;

  public BitcoindeStreamingMarketDataService(BitcoindeStreamingService streamingService) {
    this.streamingService = streamingService;
  }

  @Override
  public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
    return this.streamingService
        .subscribeChannel(currencyPair, args)
        .map(
            event -> {
              if (LOG.isTraceEnabled()) {
                LOG.trace("event: {}", event);
              }
              BitcoindeOrderbook orderbook =
                  this.orderbooks.getOrDefault(currencyPair, new BitcoindeOrderbook());

              try {
                orderbook = event.toBitcoindeOrderbook(currencyPair, orderbook);
                if (event instanceof BitcoindeOrderbookSnapshotEvent) {
                  this.orderbooks.put(currencyPair, orderbook);
                }
              } catch (Exception e) {
                return Optional.empty();
              }

              if (args == null || args.length == 0 || !(args[0] instanceof OrderbookOrdersParams)) {
                return Optional.of(adaptOrderBook(orderbook.getAsks(), orderbook.getBids()));
              } else {
                final OrderbookOrdersParams params = (OrderbookOrdersParams) args[0];
                return Optional.of(
                    adaptOrderBook(orderbook.getAsks(params), orderbook.getBids(params)));
              }
            })
        .filter(Optional::isPresent)
        .map(orderbook -> (OrderBook) orderbook.get());
  }

  @Override
  public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args) {
    throw new NotYetImplementedForExchangeException();
  }

  @Override
  public Observable<Trade> getTrades(CurrencyPair currencyPair, Object... args) {
    throw new NotYetImplementedForExchangeException();
  }
}
