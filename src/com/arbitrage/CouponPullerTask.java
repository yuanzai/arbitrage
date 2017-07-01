/**
 * Created by junyuanlau on 30/6/17.
 */

package com.arbitrage;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CouponPullerTask {
    private static final Logger logger = LoggerFactory.getLogger(ArbitrageAnalysis.class);
    static final String BTC = "btc";
    static final String LTC = "ltc";
    static final String ETH = "eth";
    static final String XRP = "xrp";
    static final String BTS = "bts";
    public static final String FIXER_FX_URL = "http://api.fixer.io/latest?base=USD";
    private static final String JUBI_BASE_URL = "https://www.jubi.com/api/v1/ticker?coin=";
    private static final String POLONIEX_URL = "https://poloniex.com/public?command=returnTicker";

    private static final String LAST = "last";

    private static final Map<String, String> JBUI_COIN_TO_API = ImmutableMap.<String, String>builder()
            .put(BTC, JUBI_BASE_URL + "btc")
            .put(LTC, JUBI_BASE_URL + "ltc")
            .put(ETH, JUBI_BASE_URL + "eth")
            .put(XRP, JUBI_BASE_URL + "xrp")
            .put(BTS, JUBI_BASE_URL + "bts")
            .build();

    private static final String OUTPUT_FOLDER = "/Users/junyuanlau/Dropbox/Arbitrage/";

    static final Map<String, Double> diff = ImmutableMap.<String, Double>builder()
            .put(BTC, 300d)
            .put(LTC, 15d)
            .put(ETH, 9d)
            .put(XRP, .06)
            .put(BTS, .06)
            .build();

    void handleData() throws Exception {
        Map<String, Double> chinaCoinPrice = getChinaPrices();
        logger.debug("China coin price : {}",chinaCoinPrice.toString());
        //double cnyPrice = getCnyPrice();
        double rmb = getRmbPrice();
        Map<String, Double> usCoinPrice = getUsCoinPrice(rmb);
        String diffResult = getDiffResult(chinaCoinPrice, rmb, usCoinPrice);
        logger.debug("US coin price : {}", usCoinPrice.toString());
        Map<String, Double> diffAnalysis = diffAnalysis(chinaCoinPrice, usCoinPrice);
        logger.debug("Percentage diff (china / us) : {}", diffAnalysis.toString());
        appendRowToFile(OUTPUT_FOLDER + "data.csv", "");

        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        if (minute == 10 || !diffResult.isEmpty()) {
            //sendEmail(chinaCoinPrice, usCoinPrice, diffResult);
        }
    }

    /**
     * Returns a map of crypto -> china/us price
     */
    private Map<String, Double> diffAnalysis(Map<String, Double> coinChinaPrice, Map<String, Double> usCoinPrice) {
        Map<String, Double> diffPercentage = new HashMap<>();
        for (String name : diff.keySet()) {
            Double china = coinChinaPrice.get(name);
            Double us = usCoinPrice.get(name);
            if (china != null && us != null) {
                diffPercentage.put(name, china / us);
            } else {
                diffPercentage.put(name, 0d);
            }
        }
        return diffPercentage;
    }

    private static String getCsvRow(Map<String, Double> diffAnalysis, Map<String, Double> coinChinaPrice, Map<String, Double> usCoinPrice) {
        List<String> csvRow = new ArrayList<>();
        return "";
    }

    private String getDiffResult(Map<String, Double> coinChinaPrice, double rmb, Map<String, Double> usCoinPrice) {
        StringBuilder sb = new StringBuilder();
        for(String name : diff.keySet()) {
            double china = coinChinaPrice.get(name);
            double us = usCoinPrice.get(name);
            if (Math.abs(china - us) <= diff.get(name)) {
                sb.append("price is close： " + name + " China: " + china + " US: " + us + " RMB: " + rmb + "\n\n");
            }
        }
        return sb.toString();
    }

    private Map<String, Double> getUsCoinPrice(double rmb) throws IOException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient httpClient = builder.build();
        HttpGet httpGet = new HttpGet(POLONIEX_URL);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity);
        response.close();
        httpClient.close();
        return getUsCoinPriceFromJsonString(rmb, result);
    }

    private Map<String, Double> getUsCoinPriceFromJsonString(double rmb, String result) {
        Map<String, Double> usCoinPrice = new HashMap<>();
        JSONObject jsonObject = new JSONObject(result);
        double btc = jsonObject.getJSONObject("USDT_BTC").getDouble(LAST);
        double ltc = jsonObject.getJSONObject("USDT_LTC").getDouble(LAST);
        double eth = jsonObject.getJSONObject("USDT_ETH").getDouble(LAST);
        double xrp = jsonObject.getJSONObject("USDT_XRP").getDouble(LAST);
        double bts = jsonObject.getJSONObject("BTC_BTS").getDouble(LAST) * btc;

        usCoinPrice.put(BTC, btc*rmb);
        usCoinPrice.put(LTC, ltc*rmb);
        usCoinPrice.put(ETH, eth*rmb);
        usCoinPrice.put(XRP, xrp*rmb);
        usCoinPrice.put(BTS, bts*rmb);
        return usCoinPrice;
    }

    Map<String, Double> getChinaPrices() throws IOException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient httpClient = builder.build();
        //http://api.btc38.com/v1/ticker.php?c=all&mk_type=cny
        //https://www.jubi.com/api/v1/ticker?coin=btc

        Map<String, Double> coinChinaPrice = new HashMap<>();

        for (Map.Entry<String, String> entry : JBUI_COIN_TO_API.entrySet()) {
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

    double getCnyPrice() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient testClient = builder.build();
        CloseableHttpResponse testResponse = null;
        HttpGet testGet = new HttpGet("http://api.btc38.com/v1/ticker.php?c=all&mk_type=cny");
        testGet.setHeader("accept", "*/*");
        JSONObject cnyObject = null;
        try {
            testResponse = testClient.execute(testGet);
            HttpEntity testEntity = testResponse.getEntity();
            String testResult = EntityUtils.toString(testEntity);
            cnyObject = new JSONObject(testResult);
            testResponse.close();
            testClient.close();

        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return cnyObject.getDouble("somestring");
    }

    double getRmbPrice() throws IOException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient exchangeRateHttpClient = builder.build();
        HttpGet exchangeGet = new HttpGet(FIXER_FX_URL);
        exchangeGet.setHeader("accept", "*/*");
        CloseableHttpResponse exchangeResponse = exchangeRateHttpClient.execute(exchangeGet);
        HttpEntity exchangeEntity = exchangeResponse.getEntity();
        String exchangeResult = EntityUtils.toString(exchangeEntity);
        JSONObject jsonObject = new JSONObject(exchangeResult);
        JSONObject rates = jsonObject.getJSONObject("rates");
        exchangeResponse.close();
        exchangeRateHttpClient.close();
        return rates.getDouble("CNY");
    }

    private static void appendRowToFile(String path, String row) throws IOException {
        FileWriter fileWriter = new FileWriter(path, true);
        fileWriter.append(row);
    }

}



