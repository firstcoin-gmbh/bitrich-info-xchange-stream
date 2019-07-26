package info.bitrich.xchangestream.bitfinex;

import com.fasterxml.jackson.databind.JsonNode;

public class BitfinexStreamingService extends BitfinexAbstractStreamingService {
    
    public BitfinexStreamingService(String apiUrl) {
        super(apiUrl, Integer.MAX_VALUE);
    }

    @Override
    protected void processAuthenticatedMessage(JsonNode message) {
        // this service do not process any private stream
    }

    @Override
    protected void auth() {
        // this service do not authenticate
    }

}
