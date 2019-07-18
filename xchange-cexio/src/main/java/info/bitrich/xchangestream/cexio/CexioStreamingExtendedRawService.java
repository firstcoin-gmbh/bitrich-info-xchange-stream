package info.bitrich.xchangestream.cexio;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import info.bitrich.xchangestream.cexio.dto.CexioWebSocketOrderbookSubscription;
import info.bitrich.xchangestream.cexio.dto.CexioWebSocketOrderbookUnsubscription;
import info.bitrich.xchangestream.cexio.dto.CexioWebSocketSubscriptionMessage;

public class CexioStreamingExtendedRawService extends CexioStreamingRawService {

    private static final Logger LOG = LoggerFactory.getLogger(CexioStreamingExtendedRawService.class);

    private static final Pattern PAIR_PATTERN = Pattern.compile(".*-([A-Za-z]+):([A-Za-z]+)");

    public static final String CHANNEL_ORDERBOOK = "order-book";
    public static final String ORDERBOOK_SUBSCRIPTION_EVENT = "order-book-subscribe";
    public static final String ORDERBOOK_UNSUBSCRIPTION_EVENT = "order-book-unsubscribe";
    public static final String MARKET_DATA_UPDATE_EVENT = "md_update";
    public static final String AUTH_EVENT = "auth";

    private boolean authenticated = false;

    public CexioStreamingExtendedRawService(String apiUrl) {
	super(apiUrl);
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) throws IOException {
	final String eventName = getEventName(message);

	if (ORDERBOOK_SUBSCRIPTION_EVENT.equalsIgnoreCase(eventName)
		|| MARKET_DATA_UPDATE_EVENT.equalsIgnoreCase(eventName)) {
	    final JsonNode dataNode = message.get("data");

	    if (dataNode != null) {
		final JsonNode pairNode = dataNode.get("pair");

		return pairNode != null ? CHANNEL_ORDERBOOK + "-" + pairNode.textValue() : null;
	    }
	}

	return super.getChannelNameFromMessage(message);
    }

    @Override
    public String getSubscriptionUniqueId(String channelName, Object... args) {
	if (args.length > 0) {
	    return channelName + "-" + args[0].toString();
	} else {
	    return channelName;
	}
    }

    @Override
    public String getSubscribeMessage(String channelName, Object... args) throws IOException {
	if (channelName.startsWith(CHANNEL_ORDERBOOK)) {
	    CexioWebSocketOrderbookSubscription subscription = null;
	    if (args.length == 1) {
		subscription = new CexioWebSocketOrderbookSubscription(((String) args[0]).split(":"));
	    } else if (args.length == 2) {
		subscription = new CexioWebSocketOrderbookSubscription(((String) args[0]).split(":"),
			Integer.parseInt(((String) args[1])));
	    } else {
		throw new IllegalArgumentException("One currency pair and one optional depth expected!");
	    }
	    CexioWebSocketSubscriptionMessage message = new CexioWebSocketSubscriptionMessage(
		    ORDERBOOK_SUBSCRIPTION_EVENT, subscription);

	    return objectMapper.writeValueAsString(message);
	}
	return super.getSubscribeMessage(channelName, args);
    }

    @Override
    public String getUnsubscribeMessage(String channelName) throws IOException {
	if (channelName == null) {
	    return null;
	}
	if (channelName.startsWith(CHANNEL_ORDERBOOK)) {
	    final String[] pair = getPairFromChannelName(channelName);
	    final CexioWebSocketSubscriptionMessage message = new CexioWebSocketSubscriptionMessage(
		    ORDERBOOK_UNSUBSCRIPTION_EVENT, new CexioWebSocketOrderbookUnsubscription(pair));

	    return objectMapper.writeValueAsString(message);
	}

	return super.getUnsubscribeMessage(channelName);
    }

    private String[] getPairFromChannelName(String channelName) {
	final Matcher matcher = PAIR_PATTERN.matcher(channelName);

	if (matcher.matches() && matcher.groupCount() == 2) {
	    return new String[] { matcher.group(1), matcher.group(2) };
	}

	return new String[0];
    }

    @Override
    protected void handleMessage(JsonNode message) {
	final String eventName = getEventName(message);

	if (ORDERBOOK_SUBSCRIPTION_EVENT.equalsIgnoreCase(eventName)
		|| MARKET_DATA_UPDATE_EVENT.equalsIgnoreCase(eventName)) {
	    String channel = getChannel(message);
	    handleChannelMessage(channel, message);
	} else if (AUTH_EVENT.equalsIgnoreCase(eventName)) {
	    super.handleMessage(message);
	    if (message.has("ok") && "ok".equals(message.get("ok").textValue())) {
		this.authenticated = true;
		LOG.debug("Authentificated successfully");
	    }
	} else {
	    super.handleMessage(message);
	}
    }

    public String getEventName(JsonNode message) {
	final JsonNode cexioMessage = message.get("e");

	if (cexioMessage != null) {
	    return cexioMessage.textValue();
	}

	return null;
    }

    public boolean isAuthenticated() {
	return authenticated;
    }

}
