package info.bitrich.xchangestream.liquid;

import static org.knowm.xchange.quoine.QuoineAdapters.adaptOrderBook;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.liquid.dto.LiquidOrderbook;
import info.bitrich.xchangestream.liquid.dto.LiquidOrderbookBuy;
import info.bitrich.xchangestream.liquid.dto.LiquidOrderbookLevel;
import info.bitrich.xchangestream.liquid.dto.LiquidOrderbookSell;
import info.bitrich.xchangestream.liquid.dto.LiquidOrderbookTransaction;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import java.util.HashMap;
import java.util.Map;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;

public class LiquidStreamingMarketDataService implements StreamingMarketDataService {

  private static final String ORDERBOOK_CHANNEL_PREFIX = "price_ladders_cash";
  private static final String PUSHER_EVENT = "updated";

  private final LiquidPusherStreamingService service;

  private final Map<CurrencyPair, LiquidOrderbook> orderbooks = new HashMap<>();

  public LiquidStreamingMarketDataService(LiquidPusherStreamingService service) {
    this.service = service;
  }

  @Override
  public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
    final String sellChannelName =
        ORDERBOOK_CHANNEL_PREFIX + getChannelPostfix(currencyPair) + "_sell";
    final String buyChannelName =
        ORDERBOOK_CHANNEL_PREFIX + getChannelPostfix(currencyPair) + "_buy";
    final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

    final Observable<LiquidOrderbookTransaction> sellSubscription =
        service
            .subscribeChannel(sellChannelName, PUSHER_EVENT)
            .map(
                data -> {
                  final LiquidOrderbookLevel[] levels =
                      mapper.readValue(data, LiquidOrderbookLevel[].class);
                  return new LiquidOrderbookSell(levels);
                });
    final Observable<LiquidOrderbookTransaction> buySubscription =
        service
            .subscribeChannel(buyChannelName, PUSHER_EVENT)
            .map(
                data -> {
                  final LiquidOrderbookLevel[] levels =
                      mapper.readValue(data, LiquidOrderbookLevel[].class);
                  return new LiquidOrderbookBuy(levels);
                });

    return Observable.merge(sellSubscription, buySubscription)
        .map(
            transaction -> {
              LiquidOrderbook orderbook =
                  transaction.toLiquidOrderBook(
                      orderbooks.getOrDefault(currencyPair, new LiquidOrderbook()));
              orderbooks.put(currencyPair, orderbook);

              return adaptOrderBook(orderbook.toQuoineOrderbook(), currencyPair);
            })
        .filter(orderbook -> orderbook.getAsks().size() == orderbook.getBids().size());
  }

  @Override
  public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args) {
    throw new NotYetImplementedForExchangeException();
  }

  @Override
  public Observable<Trade> getTrades(CurrencyPair currencyPair, Object... args) {
    throw new NotYetImplementedForExchangeException();
  }

  private String getChannelPostfix(CurrencyPair currencyPair) {
    return "_"
        + currencyPair.base.toString().toLowerCase()
        + currencyPair.counter.toString().toLowerCase();
  }
}
