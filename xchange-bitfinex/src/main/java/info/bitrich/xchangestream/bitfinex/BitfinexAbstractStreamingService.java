package info.bitrich.xchangestream.bitfinex;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.knowm.xchange.exceptions.ExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import info.bitrich.xchangestream.bitfinex.dto.BitfinexAuthRequestStatus;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketSubscriptionMessage;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketUnSubscriptionMessage;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketClientExtensionHandler;

public abstract class BitfinexAbstractStreamingService extends JsonNettyStreamingService {
    
    protected final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final String INFO = "info";
    private static final String ERROR = "error";
    private static final String CHANNEL_ID = "chanId";
    private static final String SUBSCRIBED = "subscribed";
    private static final String UNSUBSCRIBED = "unsubscribed";
    private static final String ERROR_CODE = "code";
    private static final String AUTH = "auth";
    private static final String STATUS = "status";
    private static final String MESSAGE = "msg";
    private static final String EVENT = "event";
    private static final String VERSION = "version";

    private static final int SUBSCRIPTION_FAILED = 10300;

    private final Map<String, String> subscribedChannels = new HashMap<>();
    private boolean authenticated = false;

    public BitfinexAbstractStreamingService(String apiUrl, int maxFramePayloadLength) {
        super(apiUrl, maxFramePayloadLength);
    }

    @Override
    protected WebSocketClientExtensionHandler getWebSocketClientExtensionHandler() {
        return null;
    }
    
    @Override
    public void messageHandler(String message) {
        log.trace("Received message: {}", message);
        JsonNode jsonNode;

        // Parse incoming message to JSON
        try {
            jsonNode = objectMapper.readTree(message);
        } catch (IOException e) {
            log.error("Error parsing incoming message to JSON: {}", message);
            return;
        }

        handleMessage(jsonNode);
    }

    @Override
    protected void handleMessage(JsonNode message) {

        if (message.isArray()) {
            String type = message.get(1).asText();
            if (type.equals("hb")) {
                return;
            }
        }

        JsonNode event = message.get(EVENT);
        if (event != null) {
            switch (event.textValue()) {
                case INFO:
                    JsonNode version = message.get(VERSION);
                    if (version != null) {
                        log.debug("Bitfinex websocket API version: {}.", version.intValue());
                    }
                    JsonNode code = message.get(ERROR_CODE);
                    if (code != null) {
                        log.debug("Bitfinex sent the error code {}: {}", code.intValue(), message.get(MESSAGE));
                    }
                    if (hasAuthentication()) {
                        auth();
                    }
                    break;
                case AUTH:
                    if (message.get(STATUS).textValue().equals(BitfinexAuthRequestStatus.FAILED.name())) {
                	final JsonNode error = message.get(MESSAGE);
                	authCompletable.signalError(error.toString());
                        log.error("Authentication error: {}", error);
                    }
                    if (message.get(STATUS).textValue().equals(BitfinexAuthRequestStatus.OK.name())) {
                	authCompletable.signalAuthComplete();
                	this.authenticated = true;
                        log.info("Authenticated successfully");
                    }
                    break;
                case SUBSCRIBED: {
                    String channel = message.get("channel").asText();
                    String pair = message.get("pair").asText();
                    String channelId = message.get(CHANNEL_ID).asText();
                    try {
                        String subscriptionUniqueId = getSubscriptionUniqueId(channel, pair);
                        subscribedChannels.put(channelId, subscriptionUniqueId);
                        log.debug("Register channel {}: {}", subscriptionUniqueId, channelId);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                    break;
                }
                case UNSUBSCRIBED: {
                    String channelId = message.get(CHANNEL_ID).asText();
                    subscribedChannels.remove(channelId);
                    break;
                }
                case ERROR:
                    if (message.get("code").asInt() == SUBSCRIPTION_FAILED) {
                        log.error("Error with message: " + message.get("symbol") + " " + message.get("msg"));
                        return;
                    }
                    super.handleError(message, new ExchangeException("Error code: " + message.get(ERROR_CODE).asText()));
                    break;
            }
        } else {
            try {
                if ("0".equals(getChannelNameFromMessage(message)) && message.isArray() && message.size() == 3) {
                    processAuthenticatedMessage(message);
                    return;
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to get channel name from message", e);
            }
            super.handleMessage(message);
        }
    }
    
    protected abstract void auth();

    public boolean isAuthenticated() {
	return authenticated;
    }

    protected abstract void processAuthenticatedMessage(JsonNode message);

    @Override
    public String getSubscriptionUniqueId(String channelName, Object... args) {
        if (args.length > 0) {
            return channelName + "-" + args[0].toString();
        } else {
            return channelName;
        }
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) throws IOException {
        String chanId;
        if (message.has(CHANNEL_ID)) {
            chanId = message.get(CHANNEL_ID).asText();
        } else {
            chanId = message.get(0).asText();
        }
        if (chanId == null) throw new IOException("Can't find CHANNEL_ID value");
        String subscribedChannel = subscribedChannels.get(chanId);
        if (subscribedChannel != null)
            return subscribedChannel;
        return chanId; // In case bitfinex adds new channels, just fallback to the name in the message
    }

    @Override
    public String getSubscribeMessage(String channelName, Object... args) throws IOException {
        BitfinexWebSocketSubscriptionMessage subscribeMessage = null;
        if (args.length == 1) {
            subscribeMessage =
                    new BitfinexWebSocketSubscriptionMessage(channelName, (String) args[0]);
        } else if (args.length == 3) {
            subscribeMessage =
                    new BitfinexWebSocketSubscriptionMessage(channelName, (String) args[0], (String) args[1],
                            (String) args[2]);
        }
        if (subscribeMessage == null) throw new IOException("SubscribeMessage: Insufficient arguments");

        return objectMapper.writeValueAsString(subscribeMessage);
    }

    @Override
    public String getUnsubscribeMessage(String channelName) throws IOException {
        String channelId = null;
        for (Map.Entry<String, String> entry : subscribedChannels.entrySet()) {
            if (entry.getValue().equals(channelName)) {
                channelId = entry.getKey();
                break;
            }
        }

        if (channelId == null) throw new IOException("Can't find channel unique name");

        BitfinexWebSocketUnSubscriptionMessage subscribeMessage =
                new BitfinexWebSocketUnSubscriptionMessage(channelId);
        ObjectMapper objectMapper = StreamingObjectMapperHelper.getObjectMapper();
        return objectMapper.writeValueAsString(subscribeMessage);
    }

}
