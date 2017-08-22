package com.arbitrage;

import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by junyuanlau on 20/8/17.
 */
public class ExchangeApiUtils {
    private ExchangeApiUtils(){};

    static String httpGetResponse(CloseableHttpClient httpClient, String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("accept", "*/*");
        CloseableHttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String json = EntityUtils.toString(entity);
        response.close();
        return json;
    }

    static double getAveragePriceForAmount(double amount, List<Pair<Double, Double>> list) {
        return amount / getUnitsForAmount(amount, list);
    }

    static double getUnitsForAmount(double amount, List<Pair<Double, Double>> list) {
        double units = 0;
        double remainingAmount = amount;
        for (Pair<Double, Double> pair : list) {
            double pairUnits = pair.getRight();
            double pairPrice = pair.getLeft();
            double pairAmount = pairPrice * pairUnits;
            if (pairAmount < remainingAmount) {
                remainingAmount -= pairAmount;
                units += pairUnits;
            } else {
                units += remainingAmount / pairPrice;
                break;
            }
        }
        return units;
    }

    static double getAmountForUnits(double units, List<Pair<Double, Double>> list) {
        double amount = 0;
        double remainingUnits = units;
        for (Pair<Double, Double> pair : list) {
            double pairUnits = pair.getRight();
            double pairPrice = pair.getLeft();
            double pairAmount = pairPrice * pairUnits;
            if (pairUnits < remainingUnits) {
                remainingUnits -= pairUnits;
                amount += pairAmount;
            } else {
                amount += remainingUnits * pairPrice;
                break;
            }
        }
        return amount;
    }

    public static Map<String, Double> getDepthAdjustedPrices(double amount, ListMultimap<String, Pair<Double, Double>> map) {
        Map<String, Double> result = new HashMap<>();
        for (String coin : map.keySet()) {
            List<Pair<Double, Double>> list = map.get(coin);
            double averagePrice = getAveragePriceForAmount(amount, list);
            result.put(coin, averagePrice);
        }
        return result;
    }


}
