package info.bitrich.xchangestream.bitfinex;

import com.fasterxml.jackson.databind.JsonNode;

import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.NettyStreamingService;

public class BitfinexStreamingExchange extends BitfinexAbstractStreamingExchange {
    
    private static String API_URI = "wss://api.bitfinex.com/ws/2";
    
    static {
	String apiUri = System.getProperty("xchangestream.bitfinex.BitfinexStreamingExchange.API_URI");
	if (apiUri != null) {
	    API_URI = apiUri;
	}
    }

    private BitfinexStreamingMarketDataService streamingMarketDataService;

    @Override
    protected void initServices() {
        super.initServices();
        this.streamingService = createStreamingService();
        this.streamingMarketDataService = new BitfinexStreamingMarketDataService((BitfinexStreamingService) streamingService);
    }

    @Override
    protected NettyStreamingService<JsonNode> createStreamingService() {
        BitfinexStreamingService service = new BitfinexStreamingService(API_URI);
        applyStreamingSpecification(getExchangeSpecification(), service);

        return service;
    }

    @Override
    public StreamingMarketDataService getStreamingMarketDataService() {
        return streamingMarketDataService;
    }

}
