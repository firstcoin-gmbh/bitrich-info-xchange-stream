package info.bitrich.xchangestream.cexio.dto;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.knowm.xchange.cexio.dto.marketdata.CexIODepth;

public class CexioOrderbook {

  private long id;
  private long timestamp;
  private final SortedMap<BigDecimal, BigDecimal[]> bids =
      new TreeMap<>(java.util.Collections.reverseOrder());
  private final SortedMap<BigDecimal, BigDecimal[]> asks = new TreeMap<>();

  public CexioOrderbook(long id, long timestamp, BigDecimal[][] bids, BigDecimal[][] asks) {
    this.id = id;
    this.timestamp = timestamp;
    createFromData(bids, asks);
  }

  public long getId() {
    return id;
  }

  private void createFromData(BigDecimal[][] bids, BigDecimal[][] asks) {
    Stream.of(bids).forEach(level -> this.bids.put(level[0], level));
    Stream.of(asks).forEach(level -> this.asks.put(level[0], level));
  }

  public CexIODepth toCexioDepth() {
    return new CexIODepth(
        timestamp,
        bids.entrySet().stream()
            .map(level -> Arrays.asList(level.getValue()))
            .collect(Collectors.toList()),
        asks.entrySet().stream()
            .map(level -> Arrays.asList(level.getValue()))
            .collect(Collectors.toList()));
  }

  public void updateData(long id, long timestamp, BigDecimal[][] bids, BigDecimal[][] asks) {
    if (id != this.id + 1) {
      throw new IllegalArgumentException(
          String.format("New id %s out of sync. Old id is %s.", id, this.id));
    }
    this.id = id;
    this.timestamp = timestamp;
    Stream.of(bids)
        .forEach(
            level -> {
              boolean shouldDelete = level[1].compareTo(ZERO) == 0;

              this.bids.remove(level[0]);
              if (!shouldDelete) {
                this.bids.put(level[0], level);
              }
            });
    Stream.of(asks)
        .forEach(
            level -> {
              boolean shouldDelete = level[1].compareTo(ZERO) == 0;

              this.asks.remove(level[0]);
              if (!shouldDelete) {
                this.asks.put(level[0], level);
              }
            });
  }
}
