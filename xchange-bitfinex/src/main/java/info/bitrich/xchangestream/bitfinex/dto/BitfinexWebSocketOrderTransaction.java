package info.bitrich.xchangestream.bitfinex.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class BitfinexWebSocketOrderTransaction<T> extends BitfinexWebSocketChangeTransaction {

    private T transaction;
    
    public BitfinexWebSocketOrderTransaction(Number channelId, String type, String arg, T transaction) {
        super(channelId, type, arg);
        this.transaction = transaction;
    }

    public T getTransaction() {
        return transaction;
    }
    
}
