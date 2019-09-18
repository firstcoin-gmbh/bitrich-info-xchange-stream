package info.bitrich.xchangestream.bitfinex;

import static org.knowm.xchange.bitfinex.service.BitfinexAdapters.adaptCurrencyPair;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bitfinex.service.BitfinexAdapters;
import org.knowm.xchange.bitfinex.BitfinexExchange;
import org.knowm.xchange.bitfinex.v1.dto.account.BitfinexAccountFeesResponse;
import org.knowm.xchange.bitfinex.v1.dto.marketdata.BitfinexSymbolDetail;
import org.knowm.xchange.bitfinex.v1.dto.trade.BitfinexAccountInfosResponse;
import org.knowm.xchange.bitfinex.service.BitfinexAccountService;
import org.knowm.xchange.bitfinex.service.BitfinexMarketDataServiceRaw;
import org.knowm.xchange.bitfinex.service.BitfinexTradeService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.utils.nonce.CurrentTimeNonceFactory;

import com.fasterxml.jackson.databind.JsonNode;

import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.service.netty.NettyStreamingService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import si.mazi.rescu.SynchronizedValueFactory;

public abstract class BitfinexAbstractStreamingExchange extends BitfinexExchange implements StreamingExchange {
    
    protected final SynchronizedValueFactory<Long> nonceFactory;
    protected NettyStreamingService<JsonNode> streamingService;
    
    public BitfinexAbstractStreamingExchange() {
        this(new CurrentTimeNonceFactory());
    }
    
    public BitfinexAbstractStreamingExchange(SynchronizedValueFactory<Long> nonceFactory) {
        this.nonceFactory = nonceFactory;
    }
    
    @Override
    protected void initServices() {
        super.initServices();
        this.streamingService = createStreamingService();
    }

    protected abstract NettyStreamingService<JsonNode> createStreamingService();
    
    @Override
    public SynchronizedValueFactory<Long> getNonceFactory() {
        return nonceFactory;
    }
    
    @Override
    public Completable connect(ProductSubscription... args) {
        return streamingService.connect();
    }

    @Override
    public Completable disconnect() {
        return streamingService.disconnect();
    }

    @Override
    public boolean isAlive() {
        return streamingService.isSocketOpen();
    }

    @Override
    public Observable<Throwable> reconnectFailure() {
        return streamingService.subscribeReconnectFailure();
    }

    @Override
    public Observable<Object> connectionSuccess() {
        return streamingService.subscribeConnectionSuccess();
    }

    @Override
    public void useCompressedMessages(boolean compressedMessages) {
        streamingService.useCompressedMessages(compressedMessages);
    }

    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {
        final ExchangeSpecification spec = super.getDefaultExchangeSpecification();
        spec.setShouldLoadRemoteMetaData(false);

        return spec;
    }
    
    @Override
    public void remoteInit() throws IOException, ExchangeException {
        BitfinexMarketDataServiceRaw dataService = (BitfinexMarketDataServiceRaw) this.marketDataService;
        List<CurrencyPair> currencyPairs = dataService.getExchangeSymbols();
        exchangeMetaData = BitfinexAdapters.adaptMetaData(currencyPairs, exchangeMetaData);

        // Get the last-price of each pair. It is needed to infer XChange's priceScale
        // out of Bitfinex's
        // pricePercision
        org.knowm.xchange.bitfinex.service.BitfinexMarketDataServiceRaw dataServiceV2 = new org.knowm.xchange.bitfinex.service.BitfinexMarketDataServiceRaw(
                this);
        Map<CurrencyPair, BigDecimal> lastPrices = Arrays.stream(dataServiceV2.getBitfinexTickers(currencyPairs))
                .map(org.knowm.xchange.bitfinex.service.BitfinexAdapters::adaptTicker).collect(Collectors.toMap(t -> t.getCurrencyPair(), t -> t.getLast()));

        final List<BitfinexSymbolDetail> symbolDetails = dataService.getSymbolDetails();
        
        exchangeMetaData = BitfinexAdapters.adaptMetaData(exchangeMetaData, 
                symbolDetails.stream()
                .filter(d -> lastPrices.containsKey(adaptCurrencyPair(d.getPair()))).collect(Collectors.toList()), 
                lastPrices);

        if (exchangeSpecification.getApiKey() != null && exchangeSpecification.getSecretKey() != null) {
            // Additional remoteInit configuration for authenticated instances
            BitfinexAccountService accountService = (BitfinexAccountService) this.accountService;
            final BitfinexAccountFeesResponse accountFees = accountService.getAccountFees();
            exchangeMetaData = BitfinexAdapters.adaptMetaData(accountFees, exchangeMetaData);

            BitfinexTradeService tradeService = (BitfinexTradeService) this.tradeService;
            final BitfinexAccountInfosResponse[] bitfinexAccountInfos = tradeService.getBitfinexAccountInfos();

            exchangeMetaData = BitfinexAdapters.adaptMetaData(bitfinexAccountInfos, exchangeMetaData);
        }
    }

}
