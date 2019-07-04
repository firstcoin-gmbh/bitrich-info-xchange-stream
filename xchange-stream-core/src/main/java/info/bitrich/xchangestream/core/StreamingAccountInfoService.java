package info.bitrich.xchangestream.core;

import java.util.List;

import org.knowm.xchange.dto.account.Wallet;

import io.reactivex.Observable;

public interface StreamingAccountInfoService {
    
    Observable<List<Wallet>> getWallets(Object... args);
    
}
