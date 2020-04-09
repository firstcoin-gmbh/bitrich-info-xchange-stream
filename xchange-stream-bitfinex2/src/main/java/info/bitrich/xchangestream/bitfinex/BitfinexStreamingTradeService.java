package info.bitrich.xchangestream.bitfinex;

import static info.bitrich.xchangestream.bitfinex.BitfinexUtils.*;
import static org.knowm.xchange.bitfinex.service.BitfinexAdapters.adaptOrders;
import static org.knowm.xchange.bitfinex.service.BitfinexAdapters.adaptTradeHistory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthOrder;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthPreTrade;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthTrade;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketCancelOrder;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketNewOrder;
import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketOrderTransaction;
import info.bitrich.xchangestream.core.StreamingTradeService;
import info.bitrich.xchangestream.service.netty.StreamingObjectMapperHelper;
import io.reactivex.Observable;
import java.math.BigInteger;
import java.util.function.Function;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexOrderStatusResponse;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexTradeResponse;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.OpenOrders;
import org.knowm.xchange.dto.trade.UserTrade;
import org.knowm.xchange.dto.trade.UserTrades;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.exceptions.ExchangeSecurityException;

public class BitfinexStreamingTradeService implements StreamingTradeService {

  private final BitfinexStreamingPrivateService service;

  public BitfinexStreamingTradeService(BitfinexStreamingPrivateService service) {
    this.service = service;
  }

  @Override
  public Observable<Order> getOrderChanges(CurrencyPair currencyPair, Object... args) {
    return getRawAuthenticatedOrders()
        .flatMapIterable(
            order -> {
              OpenOrders adaptedOpenOrders =
                  adaptOrders(new BitfinexOrderStatusResponse[] {adaptOrderStatusResponse(order)});
              return adaptedOpenOrders.getOpenOrders();
            });
  }

  @Override
  public Observable<UserTrade> getUserTrades(CurrencyPair currencyPair, Object... args) {
    return getUserTrades();
  }

  public Observable<UserTrade> getUserTrades() {
    return getRawAuthenticatedTrades()
        .flatMapIterable(
            trade -> {
              UserTrades adaptedUserTrades =
                  adaptTradeHistory(
                      new BitfinexTradeResponse[] {adaptTradeResponse(trade)},
                      adaptSymbol(trade.getPair()));
              return adaptedUserTrades.getUserTrades();
            });
  }

  public Observable<LimitOrder> getOpenOrders() {
    return getRawAuthenticatedOrders()
        .flatMapIterable(
            order -> {
              OpenOrders adaptedOpenOrders =
                  adaptOrders(new BitfinexOrderStatusResponse[] {adaptOrderStatusResponse(order)});
              return adaptedOpenOrders.getOpenOrders();
            });
  }

  public void placeLimitOrder(LimitOrder limitOrder) {
    final BitfinexWebSocketNewOrder newOrder = new BitfinexWebSocketNewOrder(limitOrder);
    final BitfinexWebSocketOrderTransaction<BitfinexWebSocketNewOrder> transaction =
        new BitfinexWebSocketOrderTransaction<>(AUTH_CHANNEL_ID, "on", null, newOrder);
    final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

    try {
      sendMessage(mapper.writeValueAsString(transaction));
    } catch (JsonProcessingException e) {
      throw new ExchangeException(String.format("Could not place %s", limitOrder), e);
    }
  }

  public void cancelOrder(String orderId) {
    final BitfinexWebSocketCancelOrder cancelOrder =
        new BitfinexWebSocketCancelOrder(new BigInteger(orderId));
    final BitfinexWebSocketOrderTransaction<BitfinexWebSocketCancelOrder> transaction =
        new BitfinexWebSocketOrderTransaction<>(AUTH_CHANNEL_ID, "oc", null, cancelOrder);
    final ObjectMapper mapper = StreamingObjectMapperHelper.getObjectMapper();

    try {
      sendMessage(mapper.writeValueAsString(transaction));
    } catch (JsonProcessingException e) {
      throw new ExchangeException(
          String.format("Could not cancel order with id \"%s\"", orderId), e);
    }
  }

  public Observable<BitfinexWebSocketAuthOrder> getRawAuthenticatedOrders() {
    return withAuthenticatedService(BitfinexStreamingPrivateService::getAuthenticatedOrders);
  }

  public Observable<BitfinexWebSocketAuthTrade> getRawAuthenticatedTrades() {
    return withAuthenticatedService(BitfinexStreamingPrivateService::getAuthenticatedTrades);
  }

  public Observable<BitfinexWebSocketAuthPreTrade> getRawAuthenticatedPreTrades() {
    return withAuthenticatedService(BitfinexStreamingPrivateService::getAuthenticatedPreTrades);
  }

  private <T> Observable<T> withAuthenticatedService(
      Function<BitfinexStreamingPrivateService, Observable<T>> serviceConsumer) {
    if (!service.isAuthenticated()) {
      throw new ExchangeSecurityException("Not authenticated");
    }
    return serviceConsumer.apply(service);
  }

  private void sendMessage(String message) {
    if (!service.isAuthenticated()) {
      throw new ExchangeSecurityException("Not authenticated");
    }
    service.sendMessage(message);
  }
}
