package info.bitrich.xchangestream.liquid.dto;

public abstract class LiquidOrderbookTransaction {

  protected final LiquidOrderbookLevel[] levels;

  public LiquidOrderbookTransaction(LiquidOrderbookLevel[] levels) {
    this.levels = levels;
  }

  public LiquidOrderbookLevel[] getLevels() {
    return levels;
  }

  public abstract LiquidOrderbook toLiquidOrderBook(LiquidOrderbook orderbook);
}
