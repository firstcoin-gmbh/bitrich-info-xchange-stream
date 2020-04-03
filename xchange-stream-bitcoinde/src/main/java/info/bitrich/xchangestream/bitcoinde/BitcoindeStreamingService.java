package info.bitrich.xchangestream.bitcoinde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderAdded;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderRemoved;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderTransaction;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.service.ConnectableService;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.knowm.xchange.exceptions.ExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitcoindeStreamingService extends ConnectableService {

  private static final Logger LOG = LoggerFactory.getLogger(BitcoindeStreamingService.class);

  private static final String REGISTER_WEBSOCKET_URL = "https://ws.bitcoin.de/socket.io/1/?t=%s";
  private static final String WEBSOCKET_URL = "wss://ws.bitcoin.de/socket.io/1/websocket/%s";

  private class BitcoindeNettyStreamingService extends JsonNettyStreamingService {

    private static final char OPEN_BRACET = '{';

    private static final String FIELD_ARGS = "args";
    private static final String FIELD_EVENT = "name";
    private static final String EVENT_ORDER_ADDED = "add_order";
    private static final String EVENT_ORDER_REMOVED = "remove_order";

    protected final PublishSubject<BitcoindeOrderTransaction> subjectOrder =
        PublishSubject.create();

    public BitcoindeNettyStreamingService(String apiUrl) {
      super(apiUrl);
    }

    @Override
    public void messageHandler(String message) {
      LOG.trace("Received message: {}", message);

      try {
        int idx = message.indexOf(OPEN_BRACET);
        if (idx >= 0) {
          handleMessage(objectMapper.readTree(message.substring(idx)));
        }
      } catch (IOException e) {
        LOG.error("Error parsing incoming message to JSON: {}", message);
      }
    }

    @Override
    protected void handleMessage(JsonNode message) {
      LOG.trace("received json: {}", message);
      final JsonNode event = message.get(FIELD_EVENT);

      if (event != null) {
        switch (event.textValue()) {
          case EVENT_ORDER_ADDED:
            handleOrderAdded(message);
            break;
          case EVENT_ORDER_REMOVED:
            handleOrderRemoved(message);
            break;
          default:
        }
      } else {
        super.handleMessage(message);
      }
    }

    private void handleOrderRemoved(JsonNode message) {
      final JsonNode args = message.get(FIELD_ARGS);

      for (JsonNode arg : args) {
        try {
          subjectOrder.onNext(objectMapper.treeToValue(arg, BitcoindeOrderRemoved.class));
        } catch (JsonProcessingException e) {
          LOG.error("Parsing of removed order failed: {}, {}", arg, e.getLocalizedMessage());
        }
      }
    }

    private void handleOrderAdded(JsonNode message) {
      final JsonNode args = message.get(FIELD_ARGS);

      for (JsonNode arg : args) {
        try {
          subjectOrder.onNext(objectMapper.treeToValue(arg, BitcoindeOrderAdded.class));
        } catch (JsonProcessingException e) {
          LOG.error("Parsing of added order failed: {}, {}", arg, e.getLocalizedMessage());
        }
      }
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) throws IOException {
      return null;
    }

    @Override
    public String getSubscribeMessage(String channelName, Object... args) throws IOException {
      return null;
    }

    @Override
    public String getUnsubscribeMessage(String channelName) throws IOException {
      return null;
    }
  }

  private final StreamingExchange streamingExchange;
  private BitcoindeNettyStreamingService delegate;

  public BitcoindeStreamingService(StreamingExchange streamingExchange) {
    this.streamingExchange = streamingExchange;
  }

  @Override
  protected Completable openConnection() {
    if (this.delegate == null || !this.delegate.isSocketOpen()) {
      try {
        this.delegate = new BitcoindeNettyStreamingService(getWebsocketUrl());
        this.streamingExchange.applyStreamingSpecification(
            this.streamingExchange.getExchangeSpecification(), this.delegate);
      } catch (Exception e) {
        return Completable.error(e);
      }
    }

    return this.delegate.connect();
  }

  private String getWebsocketUrl() throws IOException {
    final URL url =
        new URL(String.format(REGISTER_WEBSOCKET_URL, String.valueOf(System.currentTimeMillis())));
    final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    final int responseCode = conn.getResponseCode();

    if (200 <= responseCode && responseCode <= 299) {
      try (InputStream in = conn.getInputStream()) {
        String content = IOUtils.toString(in, StandardCharsets.UTF_8);
        return String.format(WEBSOCKET_URL, content.substring(0, content.indexOf(':')));
      }
    }

    throw new ExchangeException("No websocket available!");
  }

  public Completable disconnect() {
    return this.delegate.disconnect();
  }

  public boolean isSocketOpen() {
    return this.delegate.isSocketOpen();
  }

  public Observable<Throwable> reconnectFailure() {
    return this.delegate.subscribeReconnectFailure();
  }

  public Observable<Object> connectionSuccess() {
    return this.delegate.subscribeConnectionSuccess();
  }

  public void useCompressedMessages(boolean compressedMessages) {
    delegate.useCompressedMessages(compressedMessages);
  }

  Observable<BitcoindeOrderTransaction> getOrderTransactions() {
    return delegate.subjectOrder.share();
  }
}
