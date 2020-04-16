package info.bitrich.xchangestream.bitcoinde.dto;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import org.junit.Test;

public class BitcoindeOrderAddedTest {

  @Test
  public void testBitcoindeAddedOrder()
      throws JsonParseException, JsonMappingException, IOException {
    // Read in the JSON from the example resources
    final InputStream is =
        BitcoindeOrderAddedTest.class.getResourceAsStream(
            "/info/bitrich/xchangestream/bitcoinde/dto/add_order.json");

    // Use Jackson to parse it
    final ObjectMapper mapper = new ObjectMapper();
    final BitcoindeOrderAdded addedOrder = mapper.readValue(is, BitcoindeOrderAdded.class);

    assertEquals(77821706L, addedOrder.id);
    assertEquals("offer", addedOrder.type);
    assertEquals("etheur", addedOrder.tradingPair);
    assertEquals("V87F88", addedOrder.orderId);
    assertEquals(BitcoindeOrderType.SELL, addedOrder.orderType);
    assertEquals(new BigDecimal("10"), addedOrder.amount);
    assertEquals(new BigDecimal("0.51331"), addedOrder.minAmount);
    assertEquals(new BigDecimal("136.37"), addedOrder.price);
    assertEquals(new BigDecimal("1363.70"), addedOrder.volume);
    assertEquals("bronze", addedOrder.minTrustLevel);
    assertEquals(0, addedOrder.onlyKycFull);
    assertEquals(1, addedOrder.isKycFull);
    assertEquals(3, addedOrder.paymentOption);
    assertEquals("de", addedOrder.seatOfBankOfCreator);
    assertEquals(33, addedOrder.tradeToSepaCountry.split(",").length);
  }
}
