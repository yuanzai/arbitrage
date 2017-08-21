package com.arbitrage;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by junyuanlau on 4/7/17.
 */
public class PoloniexExchangeApi extends BaseExchangeApi {
    private static final Logger logger = LoggerFactory.getLogger(PoloniexExchangeApi.class);

    private static final String POLONIEX_URL = "https://poloniex.com/public?command=returnTicker";
    private static final String POLONIEX_DEPTH_URL = "https://poloniex.com/public?command=returnOrderBook&currencyPair=all&depth=10";


    static final String LAST = "last";
    static final String BID = "highestBid";
    static final String ASK = "lowestAsk";
    private final String jsonResponse;
    private final String depthJsonResponse;

    public PoloniexExchangeApi(double fxRate) throws IOException {
        super(fxRate);
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        this.jsonResponse = ExchangeApiUtils.httpGetResponse(httpClient, POLONIEX_URL);
        this.depthJsonResponse = ExchangeApiUtils.httpGetResponse(httpClient, POLONIEX_DEPTH_URL);
    }

    @Override
    Map<String, Double> getUnadjustedLastPrices() {
        return getPrice(jsonResponse, LAST);
    }

    @Override
    Map<String, Double> getUnadjustedBidPrices() {
        return getPrice(jsonResponse, BID);
    }

    @Override
    Map<String, Double> getUnadjustedAskPrices() {
        return getPrice(jsonResponse, ASK);
    }

    @Override
    public Map<String, Double> getAskBtcPrices() {
        return getBtcPrice(jsonResponse, ASK);
    }

    @Override
    public Map<String, Double> getBidBtcPrices() {
        return getBtcPrice(jsonResponse, BID);
    }

    @Override
    public Map<String, Double> getLastBtcPrices() {
        return getBtcPrice(jsonResponse, LAST);
    }

    @Override
    public ListMultimap<String, Pair<Double, Double>> getAskDepth() {
        return getDepth("asks", true);
    }

    @Override
    public ListMultimap<String, Pair<Double, Double>> getBidDepth() {
        return getDepth("bids", false);
    }

    @Override
    double getBtcPrice() {
        return getUnadjustedLastPrices().get(CouponPullerTask.BTC);
    }

    private Map<String, Double> getPrice(String result, String type) {
        Map<String, Double> usCoinPrice = new HashMap<>();
        JSONObject jsonObject = new JSONObject(result);
        double btc = jsonObject.getJSONObject("USDT_BTC").getDouble(type);
        double ltc = jsonObject.getJSONObject("USDT_LTC").getDouble(type);
        double eth = jsonObject.getJSONObject("USDT_ETH").getDouble(type);
        double xrp = jsonObject.getJSONObject("USDT_XRP").getDouble(type);
        double bts = jsonObject.getJSONObject("BTC_BTS").getDouble(type) * btc;

        usCoinPrice.put(CouponPullerTask.BTC, btc);
        usCoinPrice.put(CouponPullerTask.LTC, ltc);
        usCoinPrice.put(CouponPullerTask.ETH, eth);
        usCoinPrice.put(CouponPullerTask.XRP, xrp);
        usCoinPrice.put(CouponPullerTask.BTS, bts);
        return usCoinPrice;
    }

    private Map<String, Double> getBtcPrice(String result, String type) {
        Map<String, Double> usCoinPrice = new HashMap<>();
        JSONObject jsonObject = new JSONObject(result);
        for (String coin : CouponPullerTask.COINS) {
            if ("btc".equals(coin)) {
                usCoinPrice.put(coin, 1d);
                continue;
            }
            JSONObject pairJson = jsonObject.getJSONObject("BTC_" + coin.toUpperCase());
            usCoinPrice.put(coin, pairJson.getDouble(type));
        }
        return usCoinPrice;
    }

    public ListMultimap<String, Pair<Double, Double>> getDepth(String type, boolean sortAsc) {
        ListMultimap<String, Pair<Double, Double>> result = ArrayListMultimap.create();
            JSONObject jsonObject = new JSONObject(depthJsonResponse);

            for (String coin : CouponPullerTask.COINS) {
                if (CouponPullerTask.BTS.equals(coin)) {
                    continue;
                }
                JSONArray array = jsonObject.getJSONObject("USDT_" + coin.toUpperCase()).getJSONArray(type);
                List<Pair<Double, Double>> list = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONArray element = array.getJSONArray(i);
                    list.add(Pair.of(element.getDouble(0) * getFxRate(), element.getDouble(1)));
                }
                list.sort((a, b) -> {
                    int compare = a.getLeft().compareTo(b.getLeft());
                    return sortAsc ? compare : -compare;
                });
                result.putAll(coin, list);
            }
        return result;
    }

}
