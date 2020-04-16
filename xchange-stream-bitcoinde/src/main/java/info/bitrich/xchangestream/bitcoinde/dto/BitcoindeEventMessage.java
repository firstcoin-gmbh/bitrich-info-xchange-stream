package info.bitrich.xchangestream.bitcoinde.dto;

import java.util.Arrays;

public abstract class BitcoindeEventMessage<T> {

  public static final String EVENT_ORDER_SNAPSHOT = "snapshot_order";
  public static final String EVENT_ORDER_ADDED = "add_order";
  public static final String EVENT_ORDER_REMOVED = "remove_order";

  private String name;
  private T[] args;

  public BitcoindeEventMessage(String name, T[] args) {
    this.name = name;
    this.args = args;
  }

  public String getName() {
    return name;
  }

  public T[] getArgs() {
    return args;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder
        .append("BitcoindeEventMessage [name=")
        .append(name)
        .append(", args=")
        .append(Arrays.toString(args))
        .append("]");
    return builder.toString();
  }
}
