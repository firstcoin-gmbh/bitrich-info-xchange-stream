package info.bitrich.xchangestream.bitcoinde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeEventMessage;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderAdded;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderAddedEvent;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderRemoved;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderRemovedEvent;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderTransaction;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderbookEvent;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderbookSnapshotEvent;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.service.ConnectableService;
import info.bitrich.xchangestream.service.exception.NotConnectedException;
import info.bitrich.xchangestream.service.netty.JsonNettyStreamingService;
import info.bitrich.xchangestream.service.netty.WebSocketClientHandler;
import info.bitrich.xchangestream.service.netty.WebSocketClientHandler.WebSocketMessageHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.internal.functions.Functions;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.knowm.xchange.bitcoinde.BitcoindeUtils;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitcoindeStreamingService extends ConnectableService {

  private static final Logger LOG = LoggerFactory.getLogger(BitcoindeStreamingService.class);

  private static final String REGISTER_WEBSOCKET_URL = "https://ws.bitcoin.de/socket.io/1/?t=%s";
  private static final String WEBSOCKET_URL = "wss://ws.bitcoin.de/socket.io/1/websocket/%s";
  private static final Duration DEFAULT_RETRY_DURATION = Duration.ofSeconds(15);

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private final StreamingExchange streamingExchange;
  private final MarketDataService marketDataService;
  private BitcoindeNettyStreamingService delegate;
  private final AtomicBoolean isManualDisconnect = new AtomicBoolean();
  protected final Map<String, Subscription> bitcoindeChannels = new ConcurrentHashMap<>();
  private final List<ObservableEmitter<Throwable>> reconnFailEmitters = new LinkedList<>();
  private final List<ObservableEmitter<Object>> connectionSuccessEmitters = new LinkedList<>();
  private boolean compressedMessages = false;

  private class Subscription {

    final ObservableEmitter<BitcoindeOrderbookEvent> emitter;
    final CurrencyPair currencyPair;
    final Object[] args;

    public Subscription(
        ObservableEmitter<BitcoindeOrderbookEvent> emitter,
        CurrencyPair currencyPair,
        Object[] args) {
      this.emitter = emitter;
      this.currencyPair = currencyPair;
      this.args = args;
    }
  }

  public BitcoindeStreamingService(
      StreamingExchange streamingExchange, MarketDataService marketDataService) {
    this.streamingExchange = streamingExchange;
    this.marketDataService = marketDataService;
  }

  @Override
  protected Completable openConnection() {
    return Completable.create(
            completable -> {
              try {
                if (this.delegate == null || !this.delegate.isSocketOpen()) {
                  this.delegate = new BitcoindeNettyStreamingService(getWebsocketUrl());
                  this.streamingExchange.applyStreamingSpecification(
                      this.streamingExchange.getExchangeSpecification(), this.delegate);
                  this.delegate.setAutoReconnect(false);
                  this.delegate.useCompressedMessages(compressedMessages);
                  this.delegate
                      .subscribeReconnectFailure()
                      .subscribe(
                          t -> {
                            scheduleReconnect();
                            completable.onError(t);
                          });
                  this.delegate
                      .subscribeConnectionSuccess()
                      .subscribe(o -> completable.onComplete());
                }
                this.delegate.connect().subscribe();
              } catch (Exception throwable) {
                scheduleReconnect();
                completable.onError(throwable);
              }
            })
        .doOnError(t -> this.reconnFailEmitters.forEach(emitter -> emitter.onNext(t)))
        .doOnComplete(
            () -> {
              for (Subscription subscription : this.bitcoindeChannels.values()) {
                try {
                  subscription.emitter.onNext(
                      createSnapshotEvent(subscription.currencyPair, subscription.args));
                } catch (Exception e) {
                  subscription.emitter.onError(e);
                }
              }
              this.connectionSuccessEmitters.forEach(emitter -> emitter.onNext(new Object()));
            });
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

  private void scheduleReconnect() {
    LOG.info("Scheduling reconnection");
    scheduler.schedule(
        () ->
            connect()
                .subscribe(Functions.EMPTY_ACTION, t -> LOG.error("Reconnecting failed due to", t)),
        DEFAULT_RETRY_DURATION.toMillis(),
        TimeUnit.MILLISECONDS);
  }

  protected void reconnect() {
    if (this.isManualDisconnect.compareAndSet(true, false)) {
      // Don't attempt to reconnect
    } else {
      LOG.info("Reconnecting to new websocket because the old one was closed");
      scheduleReconnect();
    }
  }

  public Completable disconnect() {
    if (this.delegate != null) {
      this.isManualDisconnect.set(true);
      return this.delegate.disconnect();
    } else {
      return Completable.complete();
    }
  }

  public boolean isSocketOpen() {
    if (this.delegate != null) {
      return this.delegate.isSocketOpen();
    }

    return false;
  }

  public Observable<Throwable> subscribeReconnectFailure() {
    return Observable.create(reconnFailEmitters::add);
  }

  public Observable<Object> subscribeConnectionSuccess() {
    return Observable.create(connectionSuccessEmitters::add);
  }

  public void useCompressedMessages(boolean compressedMessages) {
    this.compressedMessages = compressedMessages;
  }

  protected Observable<BitcoindeOrderbookEvent> subscribeChannel(
      CurrencyPair currencyPair, Object... args) {
    final String channelName = BitcoindeUtils.createBitcoindePair(currencyPair);

    LOG.info("Subscribing to channel {}", channelName);
    return Observable.<BitcoindeOrderbookEvent>create(
            e -> {
              if (this.delegate == null || !this.delegate.isSocketOpen()) {
                e.onError(new NotConnectedException());
              }
              this.bitcoindeChannels.computeIfAbsent(
                  channelName,
                  cid -> {
                    Subscription newSubscription = new Subscription(e, currencyPair, args);
                    try {
                      e.onNext(createSnapshotEvent(currencyPair, args));
                    } catch (Exception throwable) {
                      e.onError(throwable);
                    }
                    return newSubscription;
                  });
            })
        .doOnDispose(() -> this.bitcoindeChannels.remove(channelName))
        .share();
  }

  private BitcoindeOrderbookSnapshotEvent createSnapshotEvent(
      CurrencyPair currencyPair, Object... args) throws IOException {
    final OrderBook orderbook = this.marketDataService.getOrderBook(currencyPair, args);
    final List<LimitOrder> orders = new ArrayList<>();

    orderbook.getBids().forEach(orders::add);
    orderbook.getAsks().forEach(orders::add);

    return new BitcoindeOrderbookSnapshotEvent(
        BitcoindeEventMessage.EVENT_ORDER_SNAPSHOT, orders.toArray(new LimitOrder[orders.size()]));
  }

  private class BitcoindeNettyStreamingService extends JsonNettyStreamingService {

    private static final char OPEN_BRACET = '{';
    private static final String MESSAGE_PING = "2::";
    private static final String MESSAGE_EVENT = "5::";
    private static final String FIELD_ARGS = "args";
    private static final String FIELD_EVENT = "name";
    private static final String FIELD_TRADING_PAIR = "trading_pair";

    public BitcoindeNettyStreamingService(String apiUrl) {
      super(apiUrl);
    }

    @Override
    public void messageHandler(String message) {
      LOG.trace("Received message: {}", message);

      try {
        if (message.startsWith(MESSAGE_PING)) {
          sendMessage(MESSAGE_PING);
        } else if (message.startsWith(MESSAGE_EVENT)) {
          handleMessage(objectMapper.readTree(message.substring(message.indexOf(OPEN_BRACET))));
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
        try {
          switch (event.textValue()) {
            case BitcoindeEventMessage.EVENT_ORDER_ADDED:
              handleOrderEvent(
                  objectMapper.treeToValue(message.get(FIELD_ARGS), BitcoindeOrderAdded[].class),
                  (channelName, transactions) -> {
                    if (channelName != null && bitcoindeChannels.containsKey(channelName)) {
                      bitcoindeChannels
                          .get(channelName)
                          .emitter
                          .onNext(
                              new BitcoindeOrderAddedEvent(
                                  BitcoindeEventMessage.EVENT_ORDER_ADDED,
                                  transactions.toArray(
                                      new BitcoindeOrderAdded[transactions.size()])));
                    }
                  });
              break;
            case BitcoindeEventMessage.EVENT_ORDER_REMOVED:
              handleOrderEvent(
                  objectMapper.treeToValue(message.get(FIELD_ARGS), BitcoindeOrderRemoved[].class),
                  (channelName, transactions) -> {
                    if (channelName != null && bitcoindeChannels.containsKey(channelName)) {
                      bitcoindeChannels
                          .get(channelName)
                          .emitter
                          .onNext(
                              new BitcoindeOrderRemovedEvent(
                                  BitcoindeEventMessage.EVENT_ORDER_REMOVED,
                                  transactions.toArray(
                                      new BitcoindeOrderRemoved[transactions.size()])));
                    }
                  });
              break;
            default:
          }
        } catch (JsonProcessingException e) {
          LOG.error("Parsing of message failed: {}, {}", message, e.getLocalizedMessage());
        }
      }
    }

    private void handleOrderEvent(
        BitcoindeOrderTransaction[] transactions,
        BiConsumer<String, List<BitcoindeOrderTransaction>> emitter) {
      Stream.of(transactions)
          .collect(Collectors.groupingBy(transaction -> transaction.tradingPair))
          .forEach(emitter::accept);
    }

    @Override
    protected WebSocketClientHandler getWebSocketClientHandler(
        WebSocketClientHandshaker handshaker, WebSocketMessageHandler handler) {
      return new ReconnectWebSocketClientHandler(handshaker, handler);
    }

    private class ReconnectWebSocketClientHandler extends NettyWebSocketClientHandler {

      protected ReconnectWebSocketClientHandler(
          WebSocketClientHandshaker handshaker, WebSocketMessageHandler handler) {
        super(handshaker, handler);
      }

      @Override
      public void channelInactive(ChannelHandlerContext ctx) {
        reconnect();
      }
    }

    @Override
    protected String getChannelNameFromMessage(JsonNode message) throws IOException {
      final JsonNode tradingPair = message.get(FIELD_TRADING_PAIR);

      return tradingPair != null ? tradingPair.textValue().toLowerCase() : null;
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
}
