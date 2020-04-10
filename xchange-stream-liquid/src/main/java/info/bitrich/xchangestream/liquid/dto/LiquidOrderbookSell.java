package info.bitrich.xchangestream.liquid.dto;

public class LiquidOrderbookSell extends LiquidOrderbookTransaction {

  public LiquidOrderbookSell(LiquidOrderbookLevel[] levels) {
    super(levels);
  }

  @Override
  public LiquidOrderbook toLiquidOrderBook(LiquidOrderbook orderbook) {
    orderbook.updateSellLevels(levels);
    return orderbook;
  }
}
