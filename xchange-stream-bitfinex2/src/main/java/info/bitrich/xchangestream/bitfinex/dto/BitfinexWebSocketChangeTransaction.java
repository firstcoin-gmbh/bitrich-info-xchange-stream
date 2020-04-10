package info.bitrich.xchangestream.bitfinex.dto;

public class BitfinexWebSocketChangeTransaction extends BitfinexWebSocketPrivateTransaction {

  private String arg;

  public BitfinexWebSocketChangeTransaction(Number channelId, String type, String arg) {
    super(channelId, type);
    this.arg = arg;
  }

  public String getArg() {
    return arg;
  }
}
