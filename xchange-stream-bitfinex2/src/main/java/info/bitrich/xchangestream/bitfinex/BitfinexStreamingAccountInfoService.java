package info.bitrich.xchangestream.bitfinex;

import static org.knowm.xchange.bitfinex.service.BitfinexAdapters.adaptWallets;

import info.bitrich.xchangestream.bitfinex.dto.BitfinexWebSocketAuthBalance;
import info.bitrich.xchangestream.core.StreamingAccountService;
import io.reactivex.Observable;
import java.math.BigDecimal;
import java.util.List;
import org.knowm.xchange.bitfinex.v1.dto.account.BitfinexBalancesResponse;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.exceptions.ExchangeSecurityException;

public class BitfinexStreamingAccountInfoService implements StreamingAccountService {

  private static final BigDecimal NA = new BigDecimal(-1);

  private final BitfinexStreamingPrivateService service;

  public BitfinexStreamingAccountInfoService(BitfinexStreamingPrivateService service) {
    this.service = service;
  }

  public Observable<List<Wallet>> getWallets(Object... args) {
    return getRawAuthenticatedBalances()
        .map(
            balance ->
                adaptWallets(
                    new BitfinexBalancesResponse[] {
                      new BitfinexBalancesResponse(
                          balance.getWalletType(),
                          balance.getCurrency(),
                          balance.getBalance(),
                          balance.getBalanceAvailable() != null
                              ? balance.getBalanceAvailable()
                              : NA)
                    }));
  }

  public Observable<BitfinexWebSocketAuthBalance> getRawAuthenticatedBalances() {
    if (!service.isAuthenticated()) {
      throw new ExchangeSecurityException("Not authenticated");
    }
    return service.getAuthenticatedBalances();
  }
}
