package info.bitrich.xchangestream.bitfinex;

import static org.knowm.xchange.service.BaseParamsDigest.HMAC_SHA_384;

import com.fasterxml.jackson.databind.JsonNode;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuth;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthBalance;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthOrder;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthPreTrade;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthTrade;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import org.knowm.xchange.bitfinex.service.BitfinexAdapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.mazi.rescu.SynchronizedValueFactory;

public class BitfinexStreamingPrivateService extends BitfinexAbstractStreamingService {

  private static final Logger LOG = LoggerFactory.getLogger(BitfinexStreamingPrivateService.class);

  private static final int CALCULATION_BATCH_SIZE = 8;
  private static final List<String> WALLETS = Arrays.asList("exchange", "margin", "funding");

  static final String CHANNEL_USER_POSITIONS = "userPositions";
  static final String CHANNEL_USER_BALANCE_UPDATES = "userBalanceUpdates";
  static final String CHANNEL_USER_BALANCES = "userBalances";
  static final String CHANNEL_USER_ORDER_UPDATES = "userOrderUpdates";
  static final String CHANNEL_USER_ORDERS = "userOrders";
  static final String CHANNEL_USER_TRADES = "userTrades";
  static final String CHANNEL_USER_PRE_TRADES = "userPreTrades";

  private final PublishSubject<BitfinexWebSocketAuthPreTrade> subjectPreTrade =
      PublishSubject.create();
  private final PublishSubject<BitfinexWebSocketAuthTrade> subjectTrade = PublishSubject.create();
  private final PublishSubject<BitfinexWebSocketAuthOrder> subjectOrder = PublishSubject.create();
  private final PublishSubject<BitfinexWebSocketAuthBalance> subjectBalance =
      PublishSubject.create();

  private String apiKey;
  private String apiSecret;

  private final SynchronizedValueFactory<Long> nonceFactory;

  private final BlockingQueue<String> calculationQueue = new LinkedBlockingQueue<>();
  private Disposable calculator;

  public BitfinexStreamingPrivateService(
      String apiUrl, SynchronizedValueFactory<Long> nonceFactory) {
    super(apiUrl, Integer.MAX_VALUE);
    this.nonceFactory = nonceFactory;
  }

  @Override
  public Completable connect() {
    return super.connect()
        .doOnComplete(
            () -> {
              this.calculator =
                  Observable.interval(1, TimeUnit.SECONDS).subscribe(x -> requestCalcs());
            });
  }

  @Override
  public Completable disconnect() {
    if (calculator != null) calculator.dispose();
    return super.disconnect();
  }

  @Override
  protected void processAuthenticatedMessage(JsonNode message) {
    String type = message.get(1).asText();
    JsonNode object = message.get(2);
    switch (type) {
      case "te":
        BitfinexWebSocketAuthPreTrade preTrade = BitfinexStreamingAdapters.adaptPreTrade(object);
        if (preTrade != null) subjectPreTrade.onNext(preTrade);
        break;
      case "tu":
        BitfinexWebSocketAuthTrade trade = BitfinexStreamingAdapters.adaptTrade(object);
        if (trade != null) subjectTrade.onNext(trade);
        break;
      case "os":
        BitfinexStreamingAdapters.adaptOrders(object).forEach(subjectOrder::onNext);
        break;
      case "on":
      case "ou":
      case "oc":
        BitfinexWebSocketAuthOrder order = BitfinexStreamingAdapters.adaptOrder(object);
        if (order != null) subjectOrder.onNext(order);
        break;
      case "ws":
        BitfinexStreamingAdapters.adaptBalances(object).forEach(subjectBalance::onNext);
        break;
      case "wu":
        BitfinexWebSocketAuthBalance balance = BitfinexStreamingAdapters.adaptBalance(object);
        if (balance != null) subjectBalance.onNext(balance);
        break;
      default:
        // In case bitfinex adds new channels, ignore
    }
  }

  @Override
  protected void auth() {
    long nonce = nonceFactory.createValue();
    String payload = "AUTH" + nonce;
    String signature;
    try {
      Mac macEncoder = Mac.getInstance(HMAC_SHA_384);
      SecretKeySpec secretKeySpec =
          new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_384);
      macEncoder.init(secretKeySpec);
      byte[] result = macEncoder.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      signature = DatatypeConverter.printHexBinary(result);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      log.error("auth. Sign failed error={}", e.getMessage());
      return;
    }
    BitfinexWebSocketAuth message =
        new BitfinexWebSocketAuth(apiKey, payload, String.valueOf(nonce), signature.toLowerCase());
    sendObjectMessage(message);
  }

  void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  void setApiSecret(String apiSecret) {
    this.apiSecret = apiSecret;
  }

  Observable<BitfinexWebSocketAuthOrder> getAuthenticatedOrders() {
    return subjectOrder.share();
  }

  Observable<BitfinexWebSocketAuthPreTrade> getAuthenticatedPreTrades() {
    return subjectPreTrade.share();
  }

  Observable<BitfinexWebSocketAuthTrade> getAuthenticatedTrades() {
    return subjectTrade.share();
  }

  Observable<BitfinexWebSocketAuthBalance> getAuthenticatedBalances() {
    return subjectBalance.share();
  }

  /**
   * Call on receipt of a partial balance (missing available amount) to schedule the release of a
   * full calculated amount at some point shortly.
   *
   * @param currency The currency code.
   */
  void scheduleCalculatedBalanceFetch(String currency) {
    LOG.debug("Scheduling request for full calculated balances for: {}", currency);
    calculationQueue.add(currency);
  }

  /**
   * Bitfinex generally doesn't supply calculated data, such as the available amount in a balance,
   * unless this is specifically requested. You have to send a message down the socket requesting
   * the full information. However, this is rate limited to 8 calculations a second and 30 per
   * batch, so we queue up requests and dispatch them in batches of 8, once a second. See {@link
   * #scheduleCalculatedBalanceFetch(String)}.
   *
   * <p>Details: https://docs.bitfinex.com/v2/docs/changelog#section--calc-input-message
   */
  private void requestCalcs() {
    Set<String> currencies = new HashSet<>();
    do {
      String nextRequest = calculationQueue.poll();
      if (nextRequest == null) break;
      if (currencies.size() >= CALCULATION_BATCH_SIZE) break;
      currencies.add(nextRequest);
    } while (true);

    if (currencies.isEmpty()) return;

    Object[] subscriptions =
        currencies.stream()
            .map(BitfinexAdapters::adaptBitfinexCurrency)
            .flatMap(
                currency -> WALLETS.stream().map(wallet -> "wallet_" + wallet + "_" + currency))
            .map(calcName -> new String[] {calcName})
            .toArray();
    Object[] message = new Object[] {0, "calc", null, subscriptions};

    LOG.debug("Requesting full calculated balances for: {} in {}", currencies, WALLETS);

    sendObjectMessage(message);
  }
}
