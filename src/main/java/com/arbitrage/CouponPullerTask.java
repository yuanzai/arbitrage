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
import org.apache.commons.lang3.StringUtils;
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
    public static final String FIXER_FX_URL = "http://api.fixer.io/latest?base=USD";
    static final List<String> COINS = ImmutableList.of(BTC, LTC, ETH, XRP);

    static final Map<String, Double> diff = ImmutableMap.<String, Double>builder()
            .put(BTC, 300d)
            .put(LTC, 15d)
            .put(ETH, 9d)
            .put(XRP, .06)
            .build();

    private StringBuilder logString = new StringBuilder("\n");
    private static final String ARBITRAGE_ANALYSIS_HEADER = "-------- ARBITRAGE ANALYSIS (BID/ASK) ----------";
    private static final String ARBITRAGE_TRADE_HEADER    = "---------- ARBITRAGE TRADE (BID/ASK) -----------";
    private static final String PAIR_TRADE_HEADER         = "----------- PAIR TRADE (WITH DEPTH) ------------";
    private static final String LAST_PRICE_HEADER         = "--------------- LAST PRICE COMP ----------------";


    void handleData() throws Exception {
        JubiExchangeApi cnApi = new JubiExchangeApi();
        Map<String, Double> chinaCoinPrices = cnApi.getLastPrices();
        double rmb = getRmbPrice();
        PoloniexExchangeApi usApi = new PoloniexExchangeApi(rmb);
        Map<String, Double> usCoinPrices = usApi.getLastPrices();

        BitfinexExchangeApi hkApi = new BitfinexExchangeApi(rmb);
        Map<String, Double> hkCoinPrices = hkApi.getLastPrices();

        logString.append(LAST_PRICE_HEADER).append("\n");
        diffAnalysis(chinaCoinPrices, usCoinPrices, "CN", "US");
        diffAnalysis(chinaCoinPrices, hkCoinPrices, "CN", "HK");
        diffAnalysis(cnApi.getLastBtcPrices(), usApi.getLastBtcPrices(), "CN BTC", "US BTC");

        longShortLog(usApi, cnApi, "XRP", "BTC");
        longShortCashLog(usApi, cnApi, "XRP", "ETH", 5000);
        shortXrpLongBtcLog(usApi, cnApi);
        //ArbitrageAnalysis analysis = new ArbitrageAnalysis("CN", chinaCoinPrices, "US", usCoinPrices);

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

    private void longShortLog(Exchange usApi, Exchange cnApi, String longCnCoin, String shortCnCoin) {
        logString.append(PAIR_TRADE_HEADER).append("\n");
        Positions positions = new Positions();
        double buyXrp = positions.buyAmount(cnApi, longCnCoin, "CNY", 10000d);
        logString.append(String.format("Buy %f %s for 10000 CNY @ %f\n", buyXrp, longCnCoin, 10000/buyXrp));

        double sellBtc = positions.sellAmount(cnApi, shortCnCoin, "CNY", 10000d);
        logString.append(String.format("Sell %f %s for 10000 CNY @ %f\n", sellBtc, shortCnCoin, 10000/sellBtc));

        double getBtc = positions.sellUnits(usApi, longCnCoin, shortCnCoin, buyXrp);
        logString.append(String.format("Sell %f %s for %f %s @ %f\n", buyXrp, longCnCoin, getBtc, shortCnCoin, buyXrp/getBtc));

        logString.append(positions.getPositions()).append("\n");
    }

    private void longShortCashLog(Exchange usApi, Exchange cnApi, String longCnCoin, String shortCnCoin, double ccy) {
        logString.append(PAIR_TRADE_HEADER).append("\n");
        Positions positions = new Positions();
        double buyXrp = positions.buyAmount(cnApi, longCnCoin, "CNY", ccy);
        logString.append(String.format("Buy %f %s for %f CNY @ %f\n", buyXrp, longCnCoin, ccy, ccy/buyXrp));

        double sellBtc = positions.sellAmount(cnApi, shortCnCoin, "CNY", ccy);
        logString.append(String.format("Sell %f %s for %f CNY @ %f\n", sellBtc, shortCnCoin, ccy, ccy/sellBtc));

        double sellXrp = positions.sellUnits(usApi, longCnCoin, "CNY", buyXrp);
        logString.append(String.format("Sell %f %s for %f CNY @ %f\n", buyXrp, longCnCoin, sellXrp, sellXrp/buyXrp));

        double buyBtc = positions.buyUnits(usApi, shortCnCoin, "CNY", sellBtc);
        logString.append(String.format("Buy %f %s for %f CNY @ %f\n", sellBtc, shortCnCoin, buyBtc, buyBtc/sellBtc));

        logString.append(positions.getPositions()).append("\n");
    }

    private void shortXrpLongBtcLog(Exchange usApi, Exchange cnApi) {
        logString.append(PAIR_TRADE_HEADER).append("\n");
        Positions positions = new Positions();
        double sellXrp = positions.sellAmount(cnApi, "XRP", "CNY", 10000d);
        logString.append(String.format("Sell %f XRP for 10000 CNY @ %f\n", sellXrp, 10000/sellXrp));

        double buyBtc = positions.buyAmount(cnApi, "BTC", "CNY", 10000d);
        logString.append(String.format("Buy %f BTC for 10000 CNY @ %f\n", buyBtc, 10000/buyBtc));

        double getBtc = positions.buyUnits(usApi, "XRP", "BTC", sellXrp);
        logString.append(String.format("Buy %f XRP for %f BTC @ %f\n", sellXrp, getBtc, sellXrp/getBtc));

        logString.append(positions.getPositions()).append("\n");
    }

    private void tradeLog(ArbitrageAnalysisResult result) {
        logString.append(ARBITRAGE_ANALYSIS_HEADER).append("\n");
        logString.append(result.getTrade1().toLogString()).append("\n");
        logString.append(result.getTrade2().toLogString()).append("\n");
    }

    private double simulateTradeLog(Trade trade1, Trade trade2, double amount ) {
        double buyUnitsCoin = amount / trade1.getBuyPrice();
        double sellUnitsCoin = amount / trade1.getSellPrice();
        double hedgeBuyAmount = buyUnitsCoin * trade2.getSellPrice();
        double hedgeSellAmount = sellUnitsCoin * trade2.getBuyPrice();
        double profit = hedgeBuyAmount - hedgeSellAmount;
        logString.append(ARBITRAGE_TRADE_HEADER).append("\n");
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



