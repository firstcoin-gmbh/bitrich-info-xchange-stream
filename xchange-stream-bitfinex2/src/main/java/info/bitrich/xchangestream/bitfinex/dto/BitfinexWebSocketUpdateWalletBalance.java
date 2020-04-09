package info.bitrich.xchangestream.bitfinex.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class BitfinexWebSocketUpdateWalletBalance extends BitfinexWebSocketWalletTransaction {

  public static final BitfinexWebSocketUpdateWalletBalance NIL =
      new BitfinexWebSocketUpdateWalletBalance();

  private BitfinexWebSocketWalletBalance balance;

  public BitfinexWebSocketWalletBalance getBalance() {
    return balance;
  }

  public void setBalance(BitfinexWebSocketWalletBalance balance) {
    this.balance = balance;
  }

  @Override
  public BitfinexWallet toBitfinexWallet(BitfinexWallet wallet) {
    if (wallet != null && balance != null) {
      wallet.updateWallet(balance);
    }
    return wallet;
  }
}
