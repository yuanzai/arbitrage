/**
 * Created by junyuanlau on 30/6/17.
 */

package com.arbitrage;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.StringUtils;
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

    private StringBuilder logString = new StringBuilder("\n");

    void handleData() throws Exception {
        ExchangeApi cnApi = new JubiExchangeApi();
        Map<String, Double> chinaCoinPrices = cnApi.getLastPrices();
        double rmb = getRmbPrice();
        ExchangeApi usApi = new PoloniexExchangeApi(rmb);
        Map<String, Double> usCoinPrices = usApi.getLastPrices();

        ExchangeApi hkApi = new BitfinexExchangeApi(rmb);
        Map<String, Double> hkCoinPrices = hkApi.getLastPrices();


        diffAnalysis(chinaCoinPrices, usCoinPrices, "CN", "US");
        diffAnalysis(chinaCoinPrices, hkCoinPrices, "CN", "HK");

        diffAnalysis(cnApi.getLastBtcPrices(), usApi.getLastBtcPrices(), "CN BTC", "US BTC");

        //ArbitrageAnalysis analysis = new ArbitrageAnalysis("CN", chinaCoinPrices, "US", usCoinPrices);

        logger.debug(cnApi.getBidDepthAdjustedPrices(10000d).toString());
        logger.debug(cnApi.getAskDepthAdjustedPrices(10000d).toString());

        logger.debug(usApi.getBidDepthAdjustedPrices(10000d).toString());
        logger.debug(usApi.getAskDepthAdjustedPrices(10000d).toString());

        ArbitrageAnalysis analysis = new ArbitrageAnalysis("CN", cnApi.getAskPrices(), cnApi.getBidPrices(), "US", usApi.getAskPrices(), usApi.getBidPrices());
        ArbitrageAnalysisResult result = analysis.getResult();

        tradeLog(result);
        String csvRow = CsvOutput.getCsvRow(result, chinaCoinPrices, usCoinPrices);
        simulateTradeLog(result.getTrade1(), result.getTrade2(), 1000d);
        if (!result.validate()) {
            logger.error("Results cannot be validated. Not appending to data file");
        }
        CsvOutput.appendRowToFile("data.csv", csvRow);
        logger.debug(logString.toString());
    }

    private void depthLog(ListMultimap<String, Pair<Double, Double>> bids, ListMultimap<String, Pair<Double, Double>> asks) {

    }

    private void tradeLog(ArbitrageAnalysisResult result) {
        logString.append("---------------------------\n");
        logString.append(result.getTrade1().toLogString()).append("\n");
        logString.append(result.getTrade2().toLogString()).append("\n");
    }

    private double simulateTradeLog(Trade trade1, Trade trade2, double amount ) {
        double buyUnitsCoin = amount / trade1.getBuyPrice();
        double sellUnitsCoin = amount / trade1.getSellPrice();
        double hedgeBuyAmount = buyUnitsCoin * trade2.getSellPrice();
        double hedgeSellAmount = sellUnitsCoin * trade2.getBuyPrice();
        double profit = hedgeBuyAmount - hedgeSellAmount;
        logString.append("---------------------------\n");
        logString.append("Trade value: ").append(amount).append("\n")
                .append(String.format("Buy %.2f %s | ", buyUnitsCoin, trade1.getBuy()))
                .append(String.format("Hedge %s %.2f", trade2.getSell(), hedgeBuyAmount))
                .append("\n")
                .append(String.format("Sell %.2f %s | ", sellUnitsCoin, trade1.getSell()))
                .append(String.format("Hedge %s %.2f", trade2.getBuy(), hedgeSellAmount))
                .append("\n")
                .append("Profit: ").append(profit).append("\n")
                .append("Profit margin: ").append(String.format("%.2f", profit/amount * 100)).append("%\n");

        return profit;
    }

    private void diffAnalysis(Map<String, Double> marketPrices1, Map<String, Double> marketPrices2, String market1, String market2) {
        Map<String, Double> diffAnalysis = ArbitrageAnalysis.diffAnalysis(marketPrices1, marketPrices2);
        logString.append(String.format("Percentage diff (%s / %s) : \n", market1, market2))
                .append(Joiner.on('\n').join(diffAnalysis.entrySet().stream()
                .map(entry -> String.format("%s: %.4f | %s - %.4f", entry.getKey(), entry.getValue(),
                        StringUtils.leftPad(String.format("%.4f", marketPrices1.get(entry.getKey())), 16), marketPrices2.get(entry.getKey()))).collect(Collectors.toList())))
                .append("\n");
        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        if (minute == 10) {
            //sendEmail(chinaCoinPrices, usCoinPrices, diffResult);
        }
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



