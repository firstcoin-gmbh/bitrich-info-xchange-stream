package info.bitrich.xchangestream.bitfinex.dto;

public abstract class BitfinexWebSocketPrivateTransaction {

  private Number channelId;
  private String type;

  public BitfinexWebSocketPrivateTransaction() {}

  public BitfinexWebSocketPrivateTransaction(Number channelId, String type) {
    this.channelId = channelId;
    this.type = type;
  }

  public Number getChannelId() {
    return channelId;
  }

  public void setChannelId(Number channelId) {
    this.channelId = channelId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
