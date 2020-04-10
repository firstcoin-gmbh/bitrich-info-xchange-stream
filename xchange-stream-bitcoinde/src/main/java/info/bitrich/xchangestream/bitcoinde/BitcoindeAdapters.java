package info.bitrich.xchangestream.bitcoinde;

import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderAdded;
import info.bitrich.xchangestream.bitcoinde.dto.BitcoindeOrderType;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.knowm.xchange.bitcoinde.OrderQuantities;
import org.knowm.xchange.bitcoinde.OrderRequirements;
import org.knowm.xchange.bitcoinde.TradingPartnerInformation;
import org.knowm.xchange.bitcoinde.TrustLevel;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;

public class BitcoindeAdapters {

  private BitcoindeAdapters() {}

  public static OrderType adaptOrderType(BitcoindeOrderType type) {
    if (type == BitcoindeOrderType.BUY) {
      return OrderType.BID;
    } else if (type == BitcoindeOrderType.SELL) {
      return OrderType.ASK;
    }
    throw new IllegalArgumentException(String.format("Unknown order type: %s", type.name()));
  }

  public static OrderBook adaptOrderBook(List<LimitOrder> asks, List<LimitOrder> bids) {
    Collections.sort(asks);
    Collections.sort(bids);

    return new OrderBook(null, asks, bids);
  }

  public static LimitOrder createOrder(
      CurrencyPair currencyPair,
      BitcoindeOrderAdded bitcoindeOrder,
      OrderType orderType,
      Date timeStamp) {
    final LimitOrder limitOrder =
        new LimitOrder(
            orderType,
            bitcoindeOrder.amount,
            currencyPair,
            bitcoindeOrder.orderId,
            timeStamp,
            bitcoindeOrder.price);

    limitOrder.addOrderFlag(
        new OrderQuantities(
            bitcoindeOrder.minAmount, bitcoindeOrder.amount, null, bitcoindeOrder.volume));
    // Trading partner info
    final TradingPartnerInformation tpi =
        new TradingPartnerInformation(bitcoindeOrder.userId, bitcoindeOrder.isKycFull > 0, null);
    tpi.setBankName(bitcoindeOrder.bicShort);
    tpi.setBic(bitcoindeOrder.bicFull);
    tpi.setSeatOfBank(bitcoindeOrder.seatOfBankOfCreator);
    limitOrder.addOrderFlag(tpi);

    // Order requirements
    final OrderRequirements or =
        new OrderRequirements(TrustLevel.valueOf(bitcoindeOrder.minTrustLevel.toUpperCase()));
    or.setOnlyFullyIdentified(bitcoindeOrder.onlyKycFull > 0);
    or.setPaymentOption(bitcoindeOrder.paymentOption);
    or.setSeatsOfBank(bitcoindeOrder.tradeToSepaCountry);
    limitOrder.addOrderFlag(or);

    return limitOrder;
  }
}
