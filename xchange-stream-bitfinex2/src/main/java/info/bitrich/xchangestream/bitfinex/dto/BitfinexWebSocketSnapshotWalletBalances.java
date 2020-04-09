package info.bitrich.xchangestream.bitfinex.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class BitfinexWebSocketSnapshotWalletBalances extends BitfinexWebSocketWalletTransaction {

  private BitfinexWebSocketWalletBalance[] balances;

  public BitfinexWebSocketWalletBalance[] getBalances() {
    return balances;
  }

  public void setBalances(BitfinexWebSocketWalletBalance[] balances) {
    this.balances = balances;
  }

  @Override
  public BitfinexWallet toBitfinexWallet(BitfinexWallet wallet) {
    return new BitfinexWallet(balances != null ? balances : new BitfinexWebSocketWalletBalance[0]);
  }
}
