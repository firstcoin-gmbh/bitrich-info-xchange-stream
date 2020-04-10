package info.bitrich.xchangestream.liquid;

import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import info.bitrich.xchangestream.service.pusher.PusherStreamingService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LiquidPusherStreamingService extends PusherStreamingService {

  private final Logger LOG = LoggerFactory.getLogger(LiquidPusherStreamingService.class);

  private final List<ObservableEmitter<Throwable>> reconnFailEmitters = new LinkedList<>();
  private final List<ObservableEmitter<Object>> connectionSuccessEmitters = new LinkedList<>();

  protected LiquidPusherStreamingService(String apiKey, String host) {
    super(new Pusher(apiKey, new PusherOptions().setHost(host)));
  }

  @Override
  protected Completable openConnection() {
    return super.openConnection()
        .doOnError(
            t -> {
              LOG.warn("Problem with connection", t);
              reconnFailEmitters.forEach(emitter -> emitter.onNext(t));
            })
        .doOnComplete(
            () -> connectionSuccessEmitters.forEach(emitter -> emitter.onNext(new Object())));
  }

  public Observable<Throwable> subscribeReconnectFailure() {
    return Observable.create(reconnFailEmitters::add);
  }

  public Observable<Object> subscribeConnectionSuccess() {
    return Observable.create(connectionSuccessEmitters::add);
  }
}
