package info.bitrich.xchangestream.cexio;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import si.mazi.rescu.SynchronizedValueFactory;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.cexio.CexIOExchange;
import org.knowm.xchange.utils.nonce.CurrentTimeNonceFactory;

public class CexioStreamingExchange extends CexIOExchange implements StreamingExchange {

    private static final String API_URI = "wss://ws.cex.io/ws/";

    private SynchronizedValueFactory<Long> nonceFactory = new CurrentTimeNonceFactory();

    private final CexioStreamingExtendedRawService streamingRawService;
    private final CexioStreamingMarketDataService streamingMarketDataService;

    public CexioStreamingExchange() {
	this.streamingRawService = new CexioStreamingExtendedRawService(API_URI);
	this.streamingMarketDataService = new CexioStreamingMarketDataService(streamingRawService);
    }

    @Override
    public SynchronizedValueFactory<Long> getNonceFactory() {
	return nonceFactory;
    }

    @Override
    public Completable connect(ProductSubscription... args) {
	return streamingRawService.connect();
    }
    
    @Override
    public Observable<Throwable> reconnectFailure() {
	return streamingRawService.subscribeReconnectFailure();
    }

    @Override
    public Observable<Object> connectionSuccess() {
	return streamingRawService.subscribeConnectionSuccess();
    }

    @Override
    public Completable disconnect() {
	return streamingRawService.disconnect();
    }

    @Override
    public boolean isAlive() {
	return streamingRawService.isSocketOpen();
    }

    @Override
    public StreamingMarketDataService getStreamingMarketDataService() {
	return streamingMarketDataService;
    }

    @Override
    public void useCompressedMessages(boolean compressedMessages) {
	streamingRawService.useCompressedMessages(compressedMessages);
    }

    public void setCredentials(String apiKey, String apiSecret) {
	streamingRawService.setApiKey(apiKey);
	streamingRawService.setApiSecret(apiSecret);
    }

    @Override
    public void applySpecification(ExchangeSpecification specification) {
	super.applySpecification(specification);
	final ExchangeSpecification finalSpec = getExchangeSpecification();
	final String apiKey = finalSpec.getApiKey();
	final String secretKey = finalSpec.getSecretKey();
	
	if (apiKey != null && secretKey != null) {
	    setCredentials(apiKey, secretKey);
	}
    }
    
    public CexioStreamingExtendedRawService getStreamingRawService() {
	return streamingRawService;
    }

}
