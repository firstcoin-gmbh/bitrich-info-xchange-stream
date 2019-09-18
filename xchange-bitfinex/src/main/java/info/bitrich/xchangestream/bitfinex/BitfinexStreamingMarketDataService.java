package info.bitrich.xchangestream.bitfinex;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.bitrich.xchangestream.bitfinex.dto.BitfinexOrderbook;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexOrderbookLevel;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketSnapshotOrderbook;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketSnapshotTrades;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketTickerTransaction;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketTradesTransaction;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketUpdateOrderbook;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebsocketUpdateTrade;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.marketdata.Trades;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;

import static org.knowm.xchange.bitfinex.service.BitfinexAdapters.adaptOrderBook;
import static org.knowm.xchange.bitfinex.service.BitfinexAdapters.adaptTicker;
import static org.knowm.xchange.bitfinex.service.BitfinexAdapters.adaptTrades;

/**
 * Created by Lukas Zaoralek on 7.11.17.
 */
class BitfinexStreamingMarketDataService implements StreamingMarketDataService {
    
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    private static final ChecksumBigDecimalToStringConverter CHECKSUM_CONVERTER = new ChecksumBigDecimalToStringConverter();

    private final BitfinexStreamingService service;
    private final List<ObservableEmitter<CurrencyPair>> checksumFailedEmitters = new LinkedList<>();

    public BitfinexStreamingMarketDataService(BitfinexStreamingService service) {
        this.service = service;
    }

    @Override
    public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
        String channelName = "book";
        final String depth = args.length > 0 ? args[0].toString() : "100";
        String pair = currencyPair.base.toString() + currencyPair.counter.toString();
        final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

        final Observable<ImmutableTriple<BitfinexOrderbook, Integer, OrderBook>> subscribedChannel = service
                .subscribeChannel(channelName, new Object[]{pair, "P0", depth})
                .observeOn(Schedulers.computation())
                .scan(ImmutableTriple.of(
                                new BitfinexOrderbook(new BitfinexOrderbookLevel[0]), (Integer) null, (OrderBook) null
                        ),
                        (immutableTriple, jsonNode) -> {
                            if ("cs".equals(jsonNode.get(1).asText())) {
                                return ImmutableTriple.of(
                                        immutableTriple.getLeft(), jsonNode.get(2).asInt(), immutableTriple.getRight()
                                );
                            } else {
                                final BitfinexOrderbook bitfinexOrderbook = (
                                        jsonNode.get(1).get(0).isArray()
                                                ? mapper.treeToValue(jsonNode, BitfinexWebSocketSnapshotOrderbook.class)
                                                : mapper.treeToValue(jsonNode, BitfinexWebSocketUpdateOrderbook.class)
                                ).toBitfinexOrderBook(immutableTriple.getLeft());
                                
                                return ImmutableTriple.of(
                                        bitfinexOrderbook,
                                        null,
                                        adaptOrderBook(bitfinexOrderbook.toBitfinexDepth(), currencyPair)
                                );
                            }
                        }
                ).share();
        
        final Disposable checksumDisposable = subscribedChannel.filter(triple -> triple.getMiddle() != null)
                .sample(1, TimeUnit.MINUTES)
                .subscribe(triple -> {
                    final int checksum = triple.getMiddle();
                    final OrderBook orderBook = triple.getRight();
                    final int bidCount = orderBook.getBids().size();
                    final int askCount = orderBook.getAsks().size();
                    final List<BigDecimal> csData = new ArrayList<>();

                    for (int i = 0; i < 25; i++) {
                        if (bidCount > i) {
                            csData.add(orderBook.getBids().get(i).getLimitPrice());
                            csData.add(orderBook.getBids().get(i).getOriginalAmount());
                        }
                        if (askCount > i) {
                            csData.add(orderBook.getAsks().get(i).getLimitPrice());
                            csData.add(orderBook.getAsks().get(i).getOriginalAmount().negate());
                        }
                    }

                    final String csStr = CHECKSUM_CONVERTER.convert(csData);
                    final CRC32 crc32 = new CRC32();
                    crc32.update(csStr.getBytes());
                    final int csCalc = (int) crc32.getValue();

                    if (csCalc != checksum) {
                        final String msg = "Invalid checksum " + csCalc + " vs " + checksum + " csStr " + csStr
                                + " csData " + csData;
                        LOG.error(msg);
                        checksumFailedEmitters.forEach(emitter -> emitter.onNext(currencyPair));
                    }
                });

        return subscribedChannel.filter(triple -> triple.getMiddle() == null)
                .map(ImmutableTriple::getRight)
                .doOnDispose(checksumDisposable::dispose);
    }

    @Override
    public Observable<CurrencyPair> checksumFailed() {
	return Observable.create(checksumFailedEmitters::add);
    }

    @Override
    public Observable<Ticker> getTicker(CurrencyPair currencyPair, Object... args) {
        String channelName = "ticker";

        String pair = currencyPair.base.toString() + currencyPair.counter.toString();
        final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

        Observable<BitfinexWebSocketTickerTransaction> subscribedChannel = service.subscribeChannel(channelName,
                new Object[]{pair})
                .map(s -> mapper.treeToValue(s, BitfinexWebSocketTickerTransaction.class));

        return subscribedChannel
                .map(s -> adaptTicker(s.toBitfinexTicker(), currencyPair));
    }

    @Override
    public Observable<Trade> getTrades(CurrencyPair currencyPair, Object... args) {
        String channelName = "trades";
        final String tradeType = args.length > 0 ? args[0].toString() : "te";

        String pair = currencyPair.base.toString() + currencyPair.counter.toString();
        final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

        Observable<BitfinexWebSocketTradesTransaction> subscribedChannel = service.subscribeChannel(channelName,
                new Object[]{pair})
                .filter(s -> s.get(1).asText().equals(tradeType))
                .map(s -> {
                    if (s.get(1).asText().equals("te") || s.get(1).asText().equals("tu")) {
                        return mapper.treeToValue(s, BitfinexWebsocketUpdateTrade.class);
                    } else return mapper.treeToValue(s, BitfinexWebSocketSnapshotTrades.class);
                });

        return subscribedChannel
                .flatMapIterable(s -> {
                    Trades adaptedTrades = adaptTrades(s.toBitfinexTrades(), currencyPair);
                    return adaptedTrades.getTrades();
                });
    }

}
