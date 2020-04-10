package info.bitrich.xchangestream.liquid.dto;

public class LiquidOrderbookBuy extends LiquidOrderbookTransaction {

  public LiquidOrderbookBuy(LiquidOrderbookLevel[] levels) {
    super(levels);
  }

  @Override
  public LiquidOrderbook toLiquidOrderBook(LiquidOrderbook orderbook) {
    orderbook.updateBuyLevels(levels);
    return orderbook;
  }
}
