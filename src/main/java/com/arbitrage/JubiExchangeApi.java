package com.arbitrage;

import com.google.common.collect.ImmutableMap;
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
public class JubiExchangeApi implements ExchangeApi {
    private static final String JUBI_BASE_URL = "https://www.jubi.com/api/v1/ticker?coin=";

    private static final Map<String, String> JUBI_COIN_TO_API = ImmutableMap.<String, String>builder()
            .put(CouponPullerTask.BTC, JUBI_BASE_URL + "btc")
            .put(CouponPullerTask.LTC, JUBI_BASE_URL + "ltc")
            .put(CouponPullerTask.ETH, JUBI_BASE_URL + "eth")
            .put(CouponPullerTask.XRP, JUBI_BASE_URL + "xrp")
            .put(CouponPullerTask.BTS, JUBI_BASE_URL + "bts")
            .build();

    @Override
    public Map<String, Double> getLastPrices() throws IOException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient httpClient = builder.build();
        //http://api.btc38.com/v1/ticker.php?c=all&mk_type=cny
        //https://www.jubi.com/api/v1/ticker?coin=btc

        Map<String, Double> coinChinaPrice = new HashMap<>();

        for (Map.Entry<String, String> entry : JUBI_COIN_TO_API.entrySet()) {
            HttpGet httpGet = new HttpGet(entry.getValue());
            httpGet.setHeader("accept", "*/*");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            JSONObject jsonObject = new JSONObject(result);
            coinChinaPrice.put(entry.getKey(), jsonObject.getDouble("buy"));
            response.close();
        }
        httpClient.close();
        return coinChinaPrice;
    }

    @Override
    public Map<String, Double> getBidPrices() {
        return null;
    }

    @Override
    public Map<String, Double> getAskPrices() {
        return null;
    }
}
