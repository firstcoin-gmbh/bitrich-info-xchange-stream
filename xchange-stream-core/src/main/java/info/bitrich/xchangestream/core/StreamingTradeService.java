package info.bitrich.xchangestream.core;

import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.UserTrade;

import io.reactivex.Observable;

public interface StreamingTradeService {
    
    Observable<LimitOrder> getOpenOrders(Object... args);
    
    Observable<UserTrade> getUserTrades(Object... args);
    
    void placeLimitOrder(LimitOrder limitOrder);
    
    void cancelOrder(String orderId);

}
