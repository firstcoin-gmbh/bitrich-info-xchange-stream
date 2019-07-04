package info.bitrich.xchangestream.bitfinex;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import info.bitrich.xchangestream.core.StreamingAccountInfoService;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.service.netty.NettyStreamingService;

public class BitfinexStreamingPrivateExchange extends BitfinexAbstractStreamingExchange {
    
    static final String API_URI = "wss://api.bitfinex.com/ws/2";

    private BitfinexStreamingTradeService streamingTradeService;
    private BitfinexStreamingAccountInfoService streamingAccountInfoService;
    
    @Override
    protected void initServices() {
        super.initServices();
        this.streamingTradeService = new BitfinexStreamingTradeService((BitfinexStreamingPrivateService) streamingService);
        this.streamingAccountInfoService = new BitfinexStreamingAccountInfoService((BitfinexStreamingPrivateService) streamingService);
    }

    @Override
    protected NettyStreamingService<JsonNode> createStreamingService() {
        BitfinexStreamingPrivateService service = new BitfinexStreamingPrivateService(API_URI, getNonceFactory());
        applyStreamingSpecification(getExchangeSpecification(), service);
        if (StringUtils.isNotEmpty(exchangeSpecification.getApiKey())) {
            service.setApiKey(exchangeSpecification.getApiKey());
            service.setApiSecret(exchangeSpecification.getSecretKey());
        }

        return service;
    }

    @Override
    public StreamingTradeService getStreamingTradeService() {
        return streamingTradeService;
    }

    @Override
    public StreamingAccountInfoService getStreamingAccountInfoService() {
        return streamingAccountInfoService;
    }

    public boolean isAuthenticatedAlive() {
        return streamingService != null && ((BitfinexStreamingPrivateService) streamingService).isAuthenticated();
    }

}
