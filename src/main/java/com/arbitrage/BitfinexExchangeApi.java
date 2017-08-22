package com.arbitrage;

import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by junyuanlau on 4/7/17.
 */
public class BitfinexExchangeApi extends BaseExchangeApi {
    private static final Logger logger = LoggerFactory.getLogger(BitfinexExchangeApi.class);

    private static final String BITFINEX_URL = "https://api.bitfinex.com/v1/pubticker/";
    static final String LAST = "last_price";
    static final String BID = "bid";
    static final String ASK = "ask";
    private final Map<String, String> responses;

    public BitfinexExchangeApi(double fxRate) throws IOException {
        super(fxRate);
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        responses = new HashMap<>();
        for (String coin : CouponPullerTask.COINS) {
            if ("BTS".equals(coin)) {
                continue;
            }

            responses.put(coin, ExchangeApiUtils.httpGetResponse(httpClient, BITFINEX_URL + coin.toLowerCase() + "usd"));
        }
    }

    @Override
    Map<String, Double> getUnadjustedLastPrices() {
        return getPrice(LAST);
    }

    @Override
    Map<String, Double> getUnadjustedBidPrices() {
        return getPrice(BID);
    }

    @Override
    Map<String, Double> getUnadjustedAskPrices() {
        return getPrice(ASK);
    }

    @Override
    double getBtcPrice() {
        return getPrice(LAST).get(CouponPullerTask.BTC);
    }

    private Map<String, Double> getPrice(String type) {
        Map<String, Double> coinPrice = new HashMap<>();
        for (String coin : CouponPullerTask.COINS) {
            if ("bts".equals(coin)) {
                coinPrice.put(coin, 1d);
                continue;
            }
            JSONObject jsonObject = new JSONObject(responses.get(coin));
            coinPrice.put(coin, jsonObject.getDouble(type));
        }
        return coinPrice;
    }

    @Override
    public ListMultimap<String, Pair<Double, Double>> getAskDepth() {
        return null;
    }

    @Override
    public ListMultimap<String, Pair<Double, Double>> getBidDepth() {
        return null;
    }

    @Override
    public ListMultimap<Pair<String, String>, Pair<Double, Double>> getPairAskDepth() {
        return null;
    }

    @Override
    public ListMultimap<Pair<String, String>, Pair<Double, Double>> getPairBidDepth() {
        return null;
    }
}
