package info.bitrich.xchangestream.cexio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import info.bitrich.xchangestream.cexio.dto.*;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.knowm.xchange.dto.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CexioStreamingRawService extends JsonNettyStreamingService {

    private static final Logger LOG = LoggerFactory.getLogger(CexioStreamingRawService.class);

    private static final Pattern PAIR_PATTERN = Pattern.compile(".*-([A-Za-z]+):([A-Za-z]+)");

    public static final String CONNECTED = "connected";
    public static final String AUTH = "auth";
    public static final String PING = "ping";
    public static final String PONG = "pong";
    public static final String ORDER = "order";
    public static final String TRANSACTION = "tx";
    public static final String CHANNEL_ORDERBOOK = "order-book";
    public static final String ORDERBOOK_SUBSCRIPTION_EVENT = "order-book-subscribe";
    public static final String ORDERBOOK_UNSUBSCRIPTION_EVENT = "order-book-unsubscribe";
    public static final String MARKET_DATA_UPDATE_EVENT = "md_update";

    private String apiKey;
    private String apiSecret;

    private PublishSubject<Order> subjectOrder = PublishSubject.create();
    private PublishSubject<CexioWebSocketTransaction> subjectTransaction = PublishSubject.create();

    public CexioStreamingRawService(String apiUrl) {
	super(apiUrl, Integer.MAX_VALUE);
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) throws IOException {
	final String eventName = getEventName(message);
	String result = null;

	if (ORDERBOOK_SUBSCRIPTION_EVENT.equalsIgnoreCase(eventName)
		|| MARKET_DATA_UPDATE_EVENT.equalsIgnoreCase(eventName)) {
	    final JsonNode dataNode = message.get("data");

	    if (dataNode != null) {
		final JsonNode pairNode = dataNode.get("pair");

		if (pairNode != null) {
		    result = CHANNEL_ORDERBOOK + "-" + pairNode.textValue();
		} else {
		    throw new IOException(String.format("No \"pair\" node found in message \"%s\"!", message));
		}
	    }
	}
	if (result == null) {
	    JsonNode oidNode = message.get("oid");

	    if (oidNode == null) {
		throw new IllegalArgumentException("Missing OID on message " + message);
	    }
	    result = oidNode.textValue();
	}

	return result;
    }
    
    public String getEventName(JsonNode message) {
	final JsonNode cexioMessage = message.get("e");

	if (cexioMessage != null) {
	    return cexioMessage.textValue();
	}

	return null;
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
	} else {
	    throw new IllegalArgumentException(
		    String.format("Cannot get subscription data for unknown channel name \"%s\"", channelName));
	}
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
	} else {
	    throw new IllegalArgumentException(
		    String.format("Cannot get subscription data for unknown channel name \"%s\"", channelName));
	}
    }
    
    private String[] getPairFromChannelName(String channelName) {
	final Matcher matcher = PAIR_PATTERN.matcher(channelName);

	if (matcher.matches() && matcher.groupCount() == 2) {
	    return new String[] { matcher.group(1), matcher.group(2) };
	}

	return new String[0];
    }

    @Override
    public void messageHandler(String message) {
	JsonNode jsonNode;
	try {
	    jsonNode = objectMapper.readTree(message);
	} catch (IOException e) {
	    LOG.error("Error parsing incoming message to JSON: {}", message);
	    subjectOrder.onError(e);
	    return;
	}
	handleMessage(jsonNode);
    }

    @Override
    protected void handleMessage(JsonNode message) {
	LOG.trace("Receiving message: {}", message);
	final String eventName = getEventName(message);

	try {
	    if (eventName != null) {
		switch (eventName) {
		case CONNECTED:
		    auth();
		    break;
		case AUTH:
		    CexioWebSocketAuthResponse response = deserialize(message, CexioWebSocketAuthResponse.class);
		    LOG.debug("Received Auth response: {}", response);
		    if (response != null) {
			if (response.isSuccess()) {
			    authCompletable.signalAuthComplete();
			} else {
			    String authErrorString = String.format("Authentication error: %s", response.getData().getError());
			    LOG.error(authErrorString);
			    authCompletable.signalError(authErrorString);
			}
		    }
		    break;
		case PING:
		    pong();
		    break;
		case ORDER:
		    try {
			CexioWebSocketOrderMessage cexioOrder = deserialize(message, CexioWebSocketOrderMessage.class);
			Order order = CexioAdapters.adaptOrder(cexioOrder.getData());
			if (LOG.isDebugEnabled()) {
			    LOG.debug("Order is updated: {}", order);
			}
			subjectOrder.onNext(order);
		    } catch (Exception e) {
			LOG.error("Order parsing error: {}", e.getMessage(), e);
			subjectOrder.onError(e);
		    }
		    break;
		case TRANSACTION:
		    try {
			CexioWebSocketTransactionMessage transaction = deserialize(message,
				CexioWebSocketTransactionMessage.class);
			if (LOG.isDebugEnabled()) {
			    LOG.debug("New transaction: {}", transaction.getData());
			}
			subjectTransaction.onNext(transaction.getData());
		    } catch (Exception e) {
			LOG.error("Transaction parsing error: {}", e.getMessage(), e);
			subjectTransaction.onError(e);
		    }
		    break;
		case ORDERBOOK_SUBSCRIPTION_EVENT:
		    JsonNode okNode = message.get("ok");
		    if (okNode.textValue().compareTo("ok") != 0) {
			String errorString = String.format("Error response for order book subscription: %s", message.toString());
			LOG.error(errorString);
			subjectOrder.onError(new IllegalArgumentException(errorString));
		    } else {
			if (LOG.isDebugEnabled()) {
			    LOG.debug("Orderbook Subscription Message: {}", message);
			}
			super.handleMessage(message);
		    }
		    break;
		case ORDERBOOK_UNSUBSCRIPTION_EVENT:
		    if (LOG.isDebugEnabled()) {
			LOG.debug("Orderbook Unsubscription Message: {}", message);
		    }
		    break;
		case MARKET_DATA_UPDATE_EVENT:
		    super.handleMessage(message);
		    break;
		}
	    }
	} catch (JsonProcessingException e) {
	    LOG.error("Json parsing error: {}", e.getMessage());
	}
    }
    
    private void auth() {
	if (apiSecret == null || apiKey == null) {
	    throw new IllegalStateException("API keys must be provided to use cexio streaming exchange");
	}
	long timestamp = System.currentTimeMillis() / 1000;
	CexioDigest cexioDigest = CexioDigest.createInstance(apiSecret);
	String signature = cexioDigest.createSignature(timestamp, apiKey);
	CexioWebSocketAuthMessage message = new CexioWebSocketAuthMessage(
		new CexioWebSocketAuth(apiKey, signature, timestamp));
	sendMessage(message);
    }

    private void pong() {
	CexioWebSocketPongMessage message = new CexioWebSocketPongMessage();
	sendMessage(message);
    }

    private void sendMessage(Object message) {
	try {
	    sendMessage(objectMapper.writeValueAsString(message));
	} catch (JsonProcessingException e) {
	    LOG.error("Error creating json message: {}", e.getMessage());
	}
    }

    public void setApiKey(String apiKey) {
	this.apiKey = apiKey;
    }

    public void setApiSecret(String apiSecret) {
	this.apiSecret = apiSecret;
    }
    
    @Override
    protected boolean hasAuthentication() {
	return StringUtils.isNotEmpty(apiKey);
    }

    private <T> T deserialize(JsonNode message, Class<T> valueType) throws JsonProcessingException {
	return objectMapper.treeToValue(message, valueType);
    }

    public Observable<Order> getOrderData() {
	return subjectOrder.share();
    }

    public Observable<CexioWebSocketTransaction> getTransactions() {
	return subjectTransaction.share();
    }

}
