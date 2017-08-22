package com.arbitrage;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.errorprone.annotations.Immutable;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by junyuanlau on 4/7/17.
 */
public class PoloniexExchangeApi extends BaseExchangeApi {
    private static final Logger logger = LoggerFactory.getLogger(PoloniexExchangeApi.class);

    private static final String POLONIEX_URL = "https://poloniex.com/public?command=returnTicker";
    private static final String POLONIEX_DEPTH_URL = "https://poloniex.com/public?command=returnOrderBook&currencyPair=all&depth=20";

    static final String LAST = "last";
    static final String BID = "highestBid";
    static final String ASK = "lowestAsk";

    static final Map<Pair<String, String>, String> PAIRS = ImmutableMap.<Pair<String, String>, String>builder()
            .put(Pair.of("BTC", "ETH"), "BTC_ETH")
            .put(Pair.of("BTC", "LTC"), "BTC_LTC")
            .put(Pair.of("BTC", "XRP"), "BTC_XRP")
            .put(Pair.of("USD", "BTC"), "USDT_BTC")
            .put(Pair.of("USD", "ETH"), "USDT_ETH")
            .put(Pair.of("USD", "LTC"), "USDT_LTC")
            .put(Pair.of("USD", "XRP"), "USDT_XRP")
            .build();

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

        usCoinPrice.put(CouponPullerTask.BTC, btc);
        usCoinPrice.put(CouponPullerTask.LTC, ltc);
        usCoinPrice.put(CouponPullerTask.ETH, eth);
        usCoinPrice.put(CouponPullerTask.XRP, xrp);
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

    @Override
    public ListMultimap<Pair<String, String>, Pair<Double, Double>> getPairBidDepth() {
        return getPairDepth("bids", false);
    }

    @Override
    public ListMultimap<Pair<String, String>, Pair<Double, Double>> getPairAskDepth() {
        return getPairDepth("asks", true);
    }

    public ListMultimap<Pair<String, String>, Pair<Double, Double>> getPairDepth(String type, boolean sortAsc) {
        ListMultimap<Pair<String, String>, Pair<Double, Double>> result = ArrayListMultimap.create();
        JSONObject jsonObject = new JSONObject(depthJsonResponse);
        for (Pair<String, String> pair : PAIRS.keySet()) {
            JSONArray array = jsonObject.getJSONObject(PAIRS.get(pair)).getJSONArray(type);
            result.putAll(pair, getPairList(sortAsc, array, 1));
            if ("USD".equals(pair.getLeft())) {
                result.putAll(Pair.of("CNY", pair.getRight()), getPairList(sortAsc, array, getFxRate()));
            }
        }
        return result;
    }

    private List<Pair<Double, Double>> getPairList(boolean sortAsc, JSONArray array, double rate) {
        List<Pair<Double, Double>> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONArray element = array.getJSONArray(i);
            list.add(Pair.of(element.getDouble(0) * rate, element.getDouble(1)));
        }
        list.sort((a, b) -> {
            int compare = a.getLeft().compareTo(b.getLeft());
            return sortAsc ? compare : -compare;
        });
        return list;
    }

}
