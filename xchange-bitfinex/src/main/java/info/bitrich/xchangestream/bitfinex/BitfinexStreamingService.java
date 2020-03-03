package info.bitrich.xchangestream.bitfinex;

import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;

import io.netty.channel.ChannelHandlerContext;
import io.reactivex.Completable;

public class BitfinexStreamingService extends BitfinexAbstractStreamingService {
    
    public BitfinexStreamingService(String apiUrl) {
	super(apiUrl, Integer.MAX_VALUE, Duration.ofSeconds(10), Duration.ofSeconds(15), 60);
    }
    
    @Override
    protected void handleIdle(ChannelHandlerContext ctx) {
	log.warn("WebSocket client detected idling, Closing.");
	ctx.close();
    }

    @Override
    public Completable connect() {
	return super.connect().doOnComplete(() -> sendMessage("{ \"event\": \"conf\", \"flags\": 131072 }"));
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
