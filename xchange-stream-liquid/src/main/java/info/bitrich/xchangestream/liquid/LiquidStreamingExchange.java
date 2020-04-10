package info.bitrich.xchangestream.liquid;

import static info.bitrich.xchangestream.service.ConnectableService.BEFORE_CONNECTION_HANDLER;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import org.knowm.xchange.quoine.QuoineExchange;

public class LiquidStreamingExchange extends QuoineExchange implements StreamingExchange {

  private static final String PUSHER_KEY = "LIQUID";
  private static final String PUSHER_HOST = "tap.liquid.com";

  private final LiquidPusherStreamingService streamingService;
  private LiquidStreamingMarketDataService streamingMarketDataService;

  public LiquidStreamingExchange() {
    this.streamingService = new LiquidPusherStreamingService(PUSHER_KEY, PUSHER_HOST);
    this.streamingMarketDataService = new LiquidStreamingMarketDataService(streamingService);
  }

  @Override
  protected void initServices() {
    super.initServices();
    streamingService.setBeforeConnectionHandler(
        (Runnable)
            getExchangeSpecification()
                .getExchangeSpecificParametersItem(BEFORE_CONNECTION_HANDLER));
  }

  @Override
  public Completable connect(ProductSubscription... args) {
    return streamingService.connect();
  }

  @Override
  public Completable disconnect() {
    return streamingService.disconnect();
  }

  @Override
  public Observable<Throwable> reconnectFailure() {
    return streamingService.subscribeReconnectFailure();
  }

  @Override
  public Observable<Object> connectionSuccess() {
    return streamingService.subscribeConnectionSuccess();
  }

  @Override
  public boolean isAlive() {
    return this.streamingService.isSocketOpen();
  }

  @Override
  public StreamingMarketDataService getStreamingMarketDataService() {
    return streamingMarketDataService;
  }

  @Override
  public void useCompressedMessages(boolean compressedMessages) {
    streamingService.useCompressedMessages(compressedMessages);
  }
}
