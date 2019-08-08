package info.bitrich.xchangestream.cexio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import info.bitrich.xchangestream.cexio.dto.CexioOrderbook;
import info.bitrich.xchangestream.cexio.dto.CexioWebSocketOrderbookSnapshot;
import info.bitrich.xchangestream.cexio.dto.CexioWebSocketOrderbookTransaction;
import info.bitrich.xchangestream.cexio.dto.CexioWebSocketOrderbookUpdate;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;

import static info.bitrich.xchangestream.cexio.CexioStreamingRawService.CHANNEL_ORDERBOOK;
import static info.bitrich.xchangestream.cexio.CexioStreamingRawService.MARKET_DATA_UPDATE_EVENT;
import static info.bitrich.xchangestream.cexio.CexioStreamingRawService.ORDERBOOK_SUBSCRIPTION_EVENT;
import static org.knowm.xchange.cexio.CexIOAdapters.adaptOrderBook;

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.knowm.xchange.cexio.dto.marketdata.CexIODepth;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CexioStreamingMarketDataService implements StreamingMarketDataService {
    
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CexioStreamingRawService service;
    
    private final Map<CurrencyPair, CexioOrderbook> orderbooks = new ConcurrentHashMap<>();
    private final List<ObservableEmitter<CurrencyPair>> checksumFailedEmitters = new LinkedList<>();

    public CexioStreamingMarketDataService(CexioStreamingRawService service) {
	this.service = service;
    }

    @Override
    public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
	final String channelName = CHANNEL_ORDERBOOK;
	final String pair = currencyPair.base.toString() + ":" + currencyPair.counter.toString();
	final String depth = args.length > 0 ? args[0].toString() : "100";
	final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

	Observable<Optional<CexioWebSocketOrderbookTransaction>> subscribedChannel = service
		.subscribeChannel(channelName, pair, depth).map(jsonNode -> {
		    final String eventName = service.getEventName(jsonNode);
		    final JsonNode dataNode = jsonNode.get("data");

		    if (ORDERBOOK_SUBSCRIPTION_EVENT.equalsIgnoreCase(eventName)) {
			return Optional.of(mapper.treeToValue(dataNode, CexioWebSocketOrderbookSnapshot.class));
		    } else if (MARKET_DATA_UPDATE_EVENT.equalsIgnoreCase(eventName)) {
			return Optional.of(mapper.treeToValue(dataNode, CexioWebSocketOrderbookUpdate.class));
		    } else {
			return Optional.empty();
		    }
		});

	return subscribedChannel.map(orderbookTransaction -> {
	    final CexioOrderbook oldOrderbook = orderbooks.getOrDefault(currencyPair, null);
	    
	    try {
		CexioOrderbook newOrderbook = oldOrderbook;
		
		if (orderbookTransaction.isPresent()) {
		    newOrderbook = orderbookTransaction.get().toCexioOrderBook(oldOrderbook);
		    orderbooks.merge(currencyPair, newOrderbook, (oldValue, newValue) -> {
			if (newValue == null || newValue.getId() > oldValue.getId()) {
			    return newValue;
			} else {
			    return oldValue;
			}
		    });
		}
		return adaptOrderBook(newOrderbook != null 
			? newOrderbook.toCexioDepth() 
				: new CexIODepth(String.format("No %s/%s orderbook available!", 
					currencyPair.base.getCurrencyCode(), 
					currencyPair.counter.getCurrencyCode())), 
				currencyPair);
	    } catch (IllegalArgumentException e) {
		LOG.error("Stale orderbook {}: {}", currencyPair, e.getMessage());
		checksumFailedEmitters.forEach(emitter -> emitter.onNext(currencyPair));
		return adaptOrderBook(oldOrderbook.toCexioDepth(), currencyPair);
	    }
	});
    }

    @Override
    public Observable<CurrencyPair> checksumFailed() {
	return Observable.create(checksumFailedEmitters::add);
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
