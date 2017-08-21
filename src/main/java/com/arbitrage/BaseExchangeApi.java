package com.arbitrage;

import com.google.common.collect.ListMultimap;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by junyuanlau on 20/8/17.
 */
public abstract class BaseExchangeApi implements ExchangeApi{

    private final double fxRate;
    public BaseExchangeApi(double fxRate) {
        this.fxRate = fxRate;
    }

    public BaseExchangeApi() {
        this.fxRate = 1d;
    }

    @Override
    public final Map<String, Double> getAskPrices() {
        return adjustMapForPrice(getUnadjustedAskPrices(), fxRate);
    }

    @Override
    public final Map<String, Double> getBidPrices() {
        return adjustMapForPrice(getUnadjustedBidPrices(), fxRate);
    }

    @Override
    public final Map<String, Double> getLastPrices() {
        return adjustMapForPrice(getUnadjustedLastPrices(), fxRate);
    }

    @Override
    public Map<String, Double> getAskBtcPrices() {
        return adjustMapForPrice(getUnadjustedAskPrices(), 1 / getBtcPrice());
    };

    @Override
    public Map<String, Double> getBidBtcPrices() {
        return adjustMapForPrice(getUnadjustedBidPrices(), 1 / getBtcPrice());
    };

    @Override
    public Map<String, Double> getLastBtcPrices() {
        return adjustMapForPrice(getUnadjustedLastPrices(), 1 / getBtcPrice());
    };

    abstract Map<String, Double> getUnadjustedLastPrices();
    abstract Map<String, Double> getUnadjustedBidPrices();
    abstract Map<String, Double> getUnadjustedAskPrices();

    abstract double getBtcPrice();

    private static Map<String, Double> adjustMapForPrice(Map<String, Double> map, double price){
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() * price));
    }

    public final double getFxRate() {
        return fxRate;
    }

    public Map<String, Double> getDepthAdjustedPrices(double amount, ListMultimap<String, Pair<Double, Double>> map) {
        Map<String, Double> result = new HashMap<>();
        for (String coin : map.keySet()) {
            List<Pair<Double, Double>> list = map.get(coin);
            double units = 0;
            double remainAmount = amount;
            for (Pair<Double, Double> pair : list) {
                double pairAmount = pair.getLeft() * pair.getRight();
                if (pairAmount < remainAmount) {
                    remainAmount -= pairAmount;
                    units += pair.getRight();
                } else {
                    units += remainAmount / pair.getLeft();
                    remainAmount = 0;
                    break;
                }
            }
            result.put(coin, amount / units);
        }
        return result;
    }

    @Override
    public Map<String, Double> getAskDepthAdjustedPrices(double amount) {
        return getDepthAdjustedPrices(amount, getAskDepth());
    }

    @Override
    public Map<String, Double> getBidDepthAdjustedPrices(double amount) {
        return getDepthAdjustedPrices(amount, getBidDepth());
    }
}
