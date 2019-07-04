package info.bitrich.xchangestream.bitfinex;

import com.fasterxml.jackson.databind.JsonNode;

import info.bitrich.xchangestream.core.StreamingMarketDataService;
import info.bitrich.xchangestream.service.netty.NettyStreamingService;

public class BitfinexStreamingExchange extends BitfinexAbstractStreamingExchange {
    
    private static final String API_URI = "wss://api-pub.bitfinex.com/ws/2";

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
