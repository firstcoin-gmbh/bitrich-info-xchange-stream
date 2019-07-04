package info.bitrich.xchangestream.bitfinex;

import com.fasterxml.jackson.databind.JsonNode;

import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuth;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthBalance;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthOrder;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthPreTrade;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthTrade;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.knowm.xchange.service.BaseParamsDigest.HMAC_SHA_384;

import si.mazi.rescu.SynchronizedValueFactory;

public class BitfinexStreamingPrivateService extends BitfinexAbstractStreamingService {
    
    static final String CHANNEL_USER_POSITIONS = "userPositions";
    static final String CHANNEL_USER_BALANCE_UPDATES = "userBalanceUpdates";
    static final String CHANNEL_USER_BALANCES = "userBalances";
    static final String CHANNEL_USER_ORDER_UPDATES = "userOrderUpdates";
    static final String CHANNEL_USER_ORDERS = "userOrders";
    static final String CHANNEL_USER_TRADES = "userTrades";
    static final String CHANNEL_USER_PRE_TRADES = "userPreTrades";

    private final PublishSubject<BitfinexWebSocketAuthPreTrade> subjectPreTrade = PublishSubject.create();
    private final PublishSubject<BitfinexWebSocketAuthTrade> subjectTrade = PublishSubject.create();
    private final PublishSubject<BitfinexWebSocketAuthOrder> subjectOrder = PublishSubject.create();
    private final PublishSubject<BitfinexWebSocketAuthBalance> subjectBalance = PublishSubject.create();

    private String apiKey;
    private String apiSecret;

    private final SynchronizedValueFactory<Long> nonceFactory;

    public BitfinexStreamingPrivateService(String apiUrl, SynchronizedValueFactory<Long> nonceFactory) {
        super(apiUrl, Integer.MAX_VALUE);
        this.nonceFactory = nonceFactory;
    }

    @Override
    protected void processAuthenticatedMessage(JsonNode message) {
        String type = message.get(1).asText();
        JsonNode object = message.get(2);
        switch (type) {
            case "te":
                BitfinexWebSocketAuthPreTrade preTrade = BitfinexStreamingAdapters.adaptPreTrade(object);
                if (preTrade != null)
                    subjectPreTrade.onNext(preTrade);
                break;
            case "tu":
                BitfinexWebSocketAuthTrade trade = BitfinexStreamingAdapters.adaptTrade(object);
                if (trade != null)
                    subjectTrade.onNext(trade);
                break;
            case "os":
                BitfinexStreamingAdapters.adaptOrders(object).forEach(subjectOrder::onNext);
                break;
            case "on":
            case "ou":
            case "oc":
                BitfinexWebSocketAuthOrder order = BitfinexStreamingAdapters.adaptOrder(object);
                if (order != null)
                    subjectOrder.onNext(order);
                break;
            case "ws":
                BitfinexStreamingAdapters.adaptBalances(object).forEach(subjectBalance::onNext);
                break;
            case "wu":
                BitfinexWebSocketAuthBalance balance = BitfinexStreamingAdapters.adaptBalance(object);
                if (balance != null)
                    subjectBalance.onNext(balance);
                break;
            default:
                // In case bitfinex adds new channels, ignore
        }
    }

    void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    @Override
    protected boolean isAuthenticated() {
        return StringUtils.isNotEmpty(apiKey);
    }

    @Override
    protected void auth() {
        long nonce = nonceFactory.createValue();
        String payload = "AUTH" + nonce;
        String signature;
        try {
            Mac macEncoder = Mac.getInstance(HMAC_SHA_384);
            SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_384);
            macEncoder.init(secretKeySpec);
            byte[] result = macEncoder.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            signature = DatatypeConverter.printHexBinary(result);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("auth. Sign failed error={}", e.getMessage());
            return;
        }
        BitfinexWebSocketAuth message = new BitfinexWebSocketAuth(
                apiKey, payload, String.valueOf(nonce), signature.toLowerCase()
        );
        sendObjectMessage(message);
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
    
}