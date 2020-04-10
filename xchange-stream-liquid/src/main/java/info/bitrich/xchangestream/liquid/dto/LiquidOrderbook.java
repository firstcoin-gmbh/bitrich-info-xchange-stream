package info.bitrich.xchangestream.liquid.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.knowm.xchange.quoine.dto.marketdata.QuoineOrderBook;

public class LiquidOrderbook {

  private final AtomicReference<LiquidOrderbookLevel[]> sells;
  private final AtomicReference<LiquidOrderbookLevel[]> buys;

  public LiquidOrderbook() {
    this(new LiquidOrderbookLevel[0], new LiquidOrderbookLevel[0]);
  }

  public LiquidOrderbook(LiquidOrderbookLevel[] sells, LiquidOrderbookLevel[] buys) {
    this.sells = new AtomicReference<>(sells);
    this.buys = new AtomicReference<>(buys);
  }

  public QuoineOrderBook toQuoineOrderbook() {
    final List<BigDecimal[]> buys0 =
        Stream.of(this.buys.get())
            .map(level -> new BigDecimal[] {level.getPrice(), level.getAmount()})
            .collect(Collectors.toList());
    final List<BigDecimal[]> sells0 =
        Stream.of(this.sells.get())
            .map(level -> new BigDecimal[] {level.getPrice(), level.getAmount()})
            .collect(Collectors.toList());

    return new QuoineOrderBook(buys0, sells0);
  }

  public void updateSellLevels(LiquidOrderbookLevel[] levels) {
    this.sells.set(levels);
  }

  public void updateBuyLevels(LiquidOrderbookLevel[] levels) {
    this.buys.set(levels);
  }
}
