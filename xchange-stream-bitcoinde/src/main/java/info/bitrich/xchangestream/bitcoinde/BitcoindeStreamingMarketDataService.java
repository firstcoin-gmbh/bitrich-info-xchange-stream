package info.bitrich.xchangestream.bitcoinde;

import static info.bitrich.xchangestream.bitcoinde.BitcoindeAdapters.*;

import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderAdded;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderRemoved;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderTransaction;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderbook;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Observable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.knowm.xchange.bitcoinde.BitcoindeUtils;
import org.knowm.xchange.bitcoinde.v4.service.OrderbookOrdersParams;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitcoindeStreamingMarketDataService implements StreamingMarketDataService {

  private static final Logger LOG =
      LoggerFactory.getLogger(BitcoindeStreamingMarketDataService.class);

  private final Map<CurrencyPair, BitcoindeOrderbook> orderbooks = new HashMap<>();
  private final BitcoindeStreamingService streamingService;
  private final MarketDataService marketDataService;

  public BitcoindeStreamingMarketDataService(
      BitcoindeStreamingService streamingService, MarketDataService marketDataService) {
    this.streamingService = streamingService;
    this.marketDataService = marketDataService;
  }

  @Override
  public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
    return subscribe(currencyPair, args)
        .map(
            ot -> {
              BitcoindeOrderbook ob = this.orderbooks.get(currencyPair);

              LOG.debug("ot: {}", ot);
              if (ot instanceof BitcoindeOrderAdded) {
                ob.addOrder(
                    ot.orderId,
                    createOrder(
                        currencyPair,
                        (BitcoindeOrderAdded) ot,
                        adaptOrderType(ot.orderType),
                        null));
              } else if ((ot instanceof BitcoindeOrderRemoved)
                  && !ob.removeOrder(ot.orderId, adaptOrderType(ot.orderType))) {
                return Optional.empty();
              }

              if (args == null || args.length == 0 || !(args[0] instanceof OrderbookOrdersParams)) {
                return Optional.of(adaptOrderBook(ob.getAsks(), ob.getBids()));
              } else {
                final OrderbookOrdersParams params = (OrderbookOrdersParams) args[0];
                return Optional.of(adaptOrderBook(ob.getAsks(params), ob.getBids(params)));
              }
            })
        .filter(Optional::isPresent)
        .map(orderbook -> (OrderBook) orderbook.get());
  }

  private Observable<BitcoindeOrderTransaction> subscribe(
      CurrencyPair currencyPair, Object... args) {
    final String tradingPair = BitcoindeUtils.createBitcoindePair(currencyPair);

    try {
      this.orderbooks.put(
          currencyPair,
          new BitcoindeOrderbook(this.marketDataService.getOrderBook(currencyPair, args)));
    } catch (IOException e) {
      throw new ExchangeException(e);
    }

    return this.streamingService
        .getOrderTransactions()
        .filter(transaction -> tradingPair.equalsIgnoreCase(transaction.tradingPair));
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
