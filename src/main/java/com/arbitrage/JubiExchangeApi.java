package com.arbitrage;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by junyuanlau on 4/7/17.
 */
public class JubiExchangeApi extends BaseExchangeApi {
    private static final String JUBI_BASE_URL = "https://www.jubi.com/api/v1/ticker?coin=";
    private static final String JUBI_DEPTH_URL = "http://www.jubi.com/api/v1/depth?coin=";
    private static final String LAST = "last";
    private static final String ASK = "buy";
    private static final String BID = "sell";

    private final Map<String, String> responses = new HashMap<>();
    private final Map<String, String> depthResponses = new HashMap<>();

    public JubiExchangeApi() throws IOException {
        //http://api.btc38.com/v1/ticker.php?c=all&mk_type=cny
        //https://www.jubi.com/api/v1/ticker?coin=btc

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        for (String coin : CouponPullerTask.COINS) {
            responses.put(coin, ExchangeApiUtils.httpGetResponse(httpClient, JUBI_BASE_URL + coin.toLowerCase()));
            depthResponses.put(coin, ExchangeApiUtils.httpGetResponse(httpClient, JUBI_DEPTH_URL + coin.toLowerCase()));
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

    public Map<String, Double> getPrice(String type) {
        Map<String, Double> coinChinaPrice = new HashMap<>();
        for (String coin : CouponPullerTask.COINS) {
            JSONObject jsonObject = new JSONObject(responses.get(coin));
            coinChinaPrice.put(coin, jsonObject.getDouble(type));
        }
        return coinChinaPrice;
    }

    @Override
    public ListMultimap<String, Pair<Double, Double>> getAskDepth() {
        return getDepth("asks", true);
    }

    @Override
    public ListMultimap<String, Pair<Double, Double>> getBidDepth() {
        return getDepth("bids", false);
    }
    public ListMultimap<String, Pair<Double, Double>> getDepth(String type, boolean sortAsc) {
        ListMultimap<String, Pair<Double, Double>> result = ArrayListMultimap.create();
        for (String coin : CouponPullerTask.COINS) {
            JSONObject jsonObject = new JSONObject(depthResponses.get(coin));
            JSONArray array = jsonObject.getJSONArray(type);
            List<Pair<Double, Double>> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONArray element = array.getJSONArray(i);
                list.add(Pair.of(element.getDouble(0) * getFxRate(), element.getDouble(1)));
            }
            list.sort((a,b) -> {
                int compare = a.getLeft().compareTo(b.getLeft());
                return sortAsc ? compare : -compare;
            });
            result.putAll(coin, list);
        }
        return result;
    }
}
