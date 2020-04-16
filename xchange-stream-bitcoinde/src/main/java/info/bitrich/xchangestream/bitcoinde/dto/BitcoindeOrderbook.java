package info.bitrich.xchangestream.bitcoinde.dto;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.knowm.xchange.bitcoinde.dto.marketdata.BitcoindeOrder;
import org.knowm.xchange.bitcoinde.dto.marketdata.BitcoindeOrderbookWrapper;
import org.knowm.xchange.bitcoinde.dto.marketdata.BitcoindeOrders;
import org.knowm.xchange.bitcoinde.v4.service.OrderbookOrdersParams;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitcoindeOrderbook {

  private static final Logger LOG = LoggerFactory.getLogger(BitcoindeOrderbook.class);

  private static final OrderbookOrdersParams ALL =
      new OrderbookOrdersParams() {

        @Override
        public boolean accept(LimitOrder order) {
          return true;
        }
      };

  private Map<String, LimitOrder> asks = new HashMap<>();
  private Map<String, LimitOrder> bids = new HashMap<>();

  public BitcoindeOrderbook() {}

  public BitcoindeOrderbook(OrderBook orderbook) {
    this(orderbook, ALL);
  }

  public BitcoindeOrderbook(OrderBook orderbook, OrderbookOrdersParams params) {
    orderbook.getBids().stream()
        .filter(params::accept)
        .forEach(order -> this.bids.put(order.getId(), order));
    orderbook.getAsks().stream()
        .filter(params::accept)
        .forEach(order -> this.asks.put(order.getId(), order));
  }

  public BitcoindeOrderbook(LimitOrder[] orders) {
    Stream.of(orders)
        .forEach(
            order -> {
              if (order.getType() == OrderType.ASK) {
                this.asks.put(order.getId(), order);
              } else if (order.getType() == OrderType.BID) {
                this.bids.put(order.getId(), order);
              }
            });
  }

  public synchronized void addOrder(String id, LimitOrder order) {
    final Map<String, LimitOrder> list = order.getType() == OrderType.BID ? this.bids : this.asks;

    if (LOG.isTraceEnabled()) {
      LOG.trace("before add: {}={}, order={}", order.getType(), list.size(), order);
    }
    list.put(id, order);
  }

  public synchronized boolean removeOrder(String id, OrderType type) {
    final Map<String, LimitOrder> list = type == OrderType.BID ? this.bids : this.asks;

    if (LOG.isTraceEnabled()) {
      LOG.trace("before remove: {}={}, containsKey={}", type, list.size(), list.containsKey(id));
    }

    return list.remove(id) != null;
  }

  public List<LimitOrder> getBids() {
    return getBids(ALL);
  }

  public synchronized List<LimitOrder> getBids(OrderbookOrdersParams params) {
    return this.bids.values().stream().filter(params::accept).collect(Collectors.toList());
  }

  public List<LimitOrder> getAsks() {
    return getAsks(ALL);
  }

  public synchronized List<LimitOrder> getAsks(OrderbookOrdersParams params) {
    return this.asks.values().stream().filter(params::accept).collect(Collectors.toList());
  }

  public synchronized BitcoindeOrderbookWrapper toBitcoindeOrderbookWrapper() {
    final List<BitcoindeOrder> askOrders = toBitcoindeOrders(this.asks.values());
    final List<BitcoindeOrder> bidOrders = toBitcoindeOrders(this.bids.values());

    return new BitcoindeOrderbookWrapper(
        new BitcoindeOrders(
            bidOrders.toArray(new BitcoindeOrder[bidOrders.size()]),
            askOrders.toArray(new BitcoindeOrder[askOrders.size()])),
        0,
        null);
  }

  private static List<BitcoindeOrder> toBitcoindeOrders(Collection<LimitOrder> orders) {
    return orders.stream()
        .collect(
            Collectors.groupingBy(
                order -> order.getLimitPrice(),
                Collectors.reducing(
                    BigDecimal.ZERO, order -> order.getOriginalAmount(), BigDecimal::add)))
        .entrySet()
        .stream()
        .map(entry -> new BitcoindeOrder(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }
}
