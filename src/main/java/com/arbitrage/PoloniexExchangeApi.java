package com.arbitrage;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by junyuanlau on 4/7/17.
 */
public class PoloniexExchangeApi implements ExchangeApi {
    private static final String POLONIEX_URL = "https://poloniex.com/public?command=returnTicker";
    private static final String LAST = "last";
    private final double rmbRate;

    public PoloniexExchangeApi(double rmbRate) {
        this.rmbRate = rmbRate;
    }

    @Override
    public Map<String, Double> getLastPrices() throws IOException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient httpClient = builder.build();
        HttpGet httpGet = new HttpGet(POLONIEX_URL);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity);
        response.close();
        httpClient.close();
        return getUsCoinPriceFromJsonString(rmbRate, result);
    }

    @Override
    public Map<String, Double> getBidPrices() {
        return null;
    }

    @Override
    public Map<String, Double> getAskPrices() {
        return null;
    }

    private Map<String, Double> getUsCoinPriceFromJsonString(double rmb, String result) {
        Map<String, Double> usCoinPrice = new HashMap<>();
        JSONObject jsonObject = new JSONObject(result);
        double btc = jsonObject.getJSONObject("USDT_BTC").getDouble(LAST);
        double ltc = jsonObject.getJSONObject("USDT_LTC").getDouble(LAST);
        double eth = jsonObject.getJSONObject("USDT_ETH").getDouble(LAST);
        double xrp = jsonObject.getJSONObject("USDT_XRP").getDouble(LAST);
        double bts = jsonObject.getJSONObject("BTC_BTS").getDouble(LAST) * btc;

        usCoinPrice.put(CouponPullerTask.BTC, btc*rmb);
        usCoinPrice.put(CouponPullerTask.LTC, ltc*rmb);
        usCoinPrice.put(CouponPullerTask.ETH, eth*rmb);
        usCoinPrice.put(CouponPullerTask.XRP, xrp*rmb);
        usCoinPrice.put(CouponPullerTask.BTS, bts*rmb);
        return usCoinPrice;
    }
}
