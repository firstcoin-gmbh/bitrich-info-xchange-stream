package info.bitrich.xchangestream.bitfinex.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knowm.xchange.bitfinex.v1.dto.account.BitfinexBalancesResponse;

public class BitfinexWallet {
    
    private static final BigDecimal NA = new BigDecimal(-1);
    
    private Map<String, Map<String, BitfinexBalancesResponse>> walletBalances = new HashMap<>();
    
    public BitfinexWallet() {
        
    }
    
    public BitfinexWallet(BitfinexWebSocketWalletBalance[] balances) {
        for (BitfinexWebSocketWalletBalance balance : balances) {
            updateWallet(balance);
        }
    }
    
    public BitfinexBalancesResponse[] toBitfinexBalancesResponses() {
        final List<BitfinexBalancesResponse> responses = new ArrayList<>();
        
        for (Map<String, BitfinexBalancesResponse> currencyBalances : walletBalances.values()) {
            responses.addAll(currencyBalances.values());
        }
         
        return responses.toArray(new BitfinexBalancesResponse[responses.size()]);
    }
    
    public void updateWallet(BitfinexWebSocketWalletBalance balance) {
        Map<String, BitfinexBalancesResponse> currencyBalances = walletBalances.get(balance.getType());
        
        if (currencyBalances == null) {
            currencyBalances = new HashMap<>();
            walletBalances.put(balance.getType(), currencyBalances);
        }
        currencyBalances.put(balance.getCurrency(), new BitfinexBalancesResponse(
                balance.getType(), 
                balance.getCurrency(), 
                balance.getBalance(), 
                balance.getBalanceAvailable() != null ? balance.getBalanceAvailable() : NA));
    }

}
