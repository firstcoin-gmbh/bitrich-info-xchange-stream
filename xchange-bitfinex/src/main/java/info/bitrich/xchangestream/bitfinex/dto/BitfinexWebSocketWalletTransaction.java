package info.bitrich.xchangestream.bitfinex.dto;

public abstract class BitfinexWebSocketWalletTransaction extends BitfinexWebSocketPrivateTransaction {
    
    public abstract BitfinexWallet toBitfinexWallet(BitfinexWallet wallet);

}
