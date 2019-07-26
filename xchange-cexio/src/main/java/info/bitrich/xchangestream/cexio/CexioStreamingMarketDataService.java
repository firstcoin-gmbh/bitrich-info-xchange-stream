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

import static info.bitrich.xchangestream.cexio.CexioStreamingRawService.CHANNEL_ORDERBOOK;
import static info.bitrich.xchangestream.cexio.CexioStreamingRawService.MARKET_DATA_UPDATE_EVENT;
import static info.bitrich.xchangestream.cexio.CexioStreamingRawService.ORDERBOOK_SUBSCRIPTION_EVENT;
import static org.knowm.xchange.cexio.CexIOAdapters.adaptOrderBook;

import java.util.HashMap;
import java.util.Map;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;

public class CexioStreamingMarketDataService implements StreamingMarketDataService {

    private final CexioStreamingRawService service;
    
    private final Map<CurrencyPair, CexioOrderbook> orderbooks = new HashMap<>();

    public CexioStreamingMarketDataService(CexioStreamingRawService service) {
	this.service = service;
    }

    @Override
    public Observable<OrderBook> getOrderBook(CurrencyPair currencyPair, Object... args) {
	final String channelName = CHANNEL_ORDERBOOK;
	final String pair = currencyPair.base.toString() + ":" + currencyPair.counter.toString();
	final String depth = args.length > 0 ? args[0].toString() : "100";
	final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

	Observable<CexioWebSocketOrderbookTransaction> subscribedChannel = service
		.subscribeChannel(channelName, pair, depth).map(s -> {
		    final String eventName = service.getEventName(s);
		    final JsonNode dataNode = s.get("data");

		    if (ORDERBOOK_SUBSCRIPTION_EVENT.equalsIgnoreCase(eventName)) {
			return mapper.treeToValue(dataNode, CexioWebSocketOrderbookSnapshot.class);
		    } else if (MARKET_DATA_UPDATE_EVENT.equalsIgnoreCase(eventName)) {
			return mapper.treeToValue(dataNode, CexioWebSocketOrderbookUpdate.class);
		    } else {
			return null;
		    }
		});

	return subscribedChannel.map(s -> {
	    CexioOrderbook cexioOrderbook = s.toCexioOrderBook(orderbooks.getOrDefault(currencyPair, null));

	    orderbooks.put(currencyPair, cexioOrderbook);

	    return adaptOrderBook(cexioOrderbook.toCexioDepth(), currencyPair);
	});
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
