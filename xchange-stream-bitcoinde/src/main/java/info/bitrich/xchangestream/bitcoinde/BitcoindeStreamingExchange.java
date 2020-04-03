package info.bitrich.xchangestream.bitcoinde;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bitcoinde.v4.BitcoindeExchange;

public class BitcoindeStreamingExchange extends BitcoindeExchange implements StreamingExchange {

  private BitcoindeStreamingService streamingService;
  private BitcoindeStreamingMarketDataService streamingMarketDataService;

  @Override
  protected void initServices() {
    super.initServices();
    this.streamingService = new BitcoindeStreamingService(this);
    this.streamingMarketDataService =
        new BitcoindeStreamingMarketDataService(streamingService, getMarketDataService());
  }

  @Override
  public ExchangeSpecification getDefaultExchangeSpecification() {
    final ExchangeSpecification spec = super.getDefaultExchangeSpecification();

    spec.setShouldLoadRemoteMetaData(false);

    return spec;
  }

  @Override
  public Completable connect(ProductSubscription... args) {
    return this.streamingService.connect();
  }

  @Override
  public Completable disconnect() {
    return this.streamingService.disconnect();
  }

  @Override
  public boolean isAlive() {
    return this.streamingService.isSocketOpen();
  }

  @Override
  public Observable<Throwable> reconnectFailure() {
    return this.streamingService.reconnectFailure();
  }

  @Override
  public Observable<Object> connectionSuccess() {
    return this.streamingService.connectionSuccess();
  }

  @Override
  public StreamingMarketDataService getStreamingMarketDataService() {
    return this.streamingMarketDataService;
  }

  @Override
  public void useCompressedMessages(boolean compressedMessages) {
    this.streamingService.useCompressedMessages(compressedMessages);
  }
}
