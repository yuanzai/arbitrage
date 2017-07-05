/**
 * Created by junyuanlau on 30/6/17.
 */

package com.arbitrage;

import java.io.IOException;
import java.util.*;

import com.google.common.collect.ImmutableList;
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

    static final List<String> COINS = ImmutableList.of(BTC, LTC, ETH, XRP, BTS);

    static final Map<String, Double> diff = ImmutableMap.<String, Double>builder()
            .put(BTC, 300d)
            .put(LTC, 15d)
            .put(ETH, 9d)
            .put(XRP, .06)
            .put(BTS, .06)
            .build();

    void handleData() throws Exception {
        ExchangeApi cnApi = new JubiExchangeApi();
        Map<String, Double> chinaCoinPrices = cnApi.getLastPrices();
        double rmb = getRmbPrice();
        ExchangeApi usApi = new PoloniexExchangeApi(rmb);
        Map<String, Double> usCoinPrices = usApi.getLastPrices();

        String diffResult = getDiffResult(chinaCoinPrices, rmb, usCoinPrices);
        Map<String, Double> diffAnalysis = ArbitrageAnalysis.diffAnalysis(chinaCoinPrices, usCoinPrices);
        logger.debug("Percentage diff (china / us) : {}", diffAnalysis.toString());

        ArbitrageAnalysis analysis = new ArbitrageAnalysis("CN", chinaCoinPrices, "US", usCoinPrices);
        ArbitrageAnalysisResult result = analysis.getResult();
        String csvRow = CsvOutput.getCsvRow(result, chinaCoinPrices, usCoinPrices);
        logger.debug(csvRow);
        if (!result.validate()) {
            logger.error("Results cannot be validated. Not appending to data file");
        }

        CsvOutput.appendRowToFile("data.csv", csvRow);
        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        if (minute == 10 || !diffResult.isEmpty()) {
            //sendEmail(chinaCoinPrices, usCoinPrices, diffResult);
        }
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

}



