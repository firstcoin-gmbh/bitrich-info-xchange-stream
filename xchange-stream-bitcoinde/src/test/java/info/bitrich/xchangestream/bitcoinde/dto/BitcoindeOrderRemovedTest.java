package info.bitrich.xchangestream.bitcoinde.dto;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import org.junit.Test;

public class BitcoindeOrderRemovedTest {

  @Test
  public void testBitcoindeRemovedOrder()
      throws JsonParseException, JsonMappingException, IOException {
    // Read in the JSON from the example resources
    final InputStream is =
        BitcoindeOrderRemovedTest.class.getResourceAsStream(
            "/info/bitrich/xchangestream/bitcoinde/dto/remove_order.json");

    // Use Jackson to parse it
    final ObjectMapper mapper = new ObjectMapper();
    final BitcoindeOrderRemoved removedOrder = mapper.readValue(is, BitcoindeOrderRemoved.class);

    assertEquals(77821675L, removedOrder.id);
    assertEquals("offer", removedOrder.type);
    assertEquals("btceur", removedOrder.tradingPair);
    assertEquals("9QXNSD", removedOrder.orderId);
    assertEquals(BitcoindeOrderType.SELL, removedOrder.orderType);
    assertEquals(new BigDecimal("3.7"), removedOrder.amount);
    assertEquals(new BigDecimal("6571.73"), removedOrder.price);
    assertEquals("0yzjXAA9kDutfQA.", removedOrder.reason);
  }
}
