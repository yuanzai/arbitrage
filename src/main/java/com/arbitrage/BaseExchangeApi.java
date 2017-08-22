package com.arbitrage;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by junyuanlau on 20/8/17.
 */
public abstract class BaseExchangeApi implements ExchangeApi, Exchange{

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

    @Override
    public Map<String, Double> getAskDepthAdjustedPrices(double amount) {
        return ExchangeApiUtils.getDepthAdjustedPrices(amount, getAskDepth());
    }

    @Override
    public Map<String, Double> getBidDepthAdjustedPrices(double amount) {
        return ExchangeApiUtils.getDepthAdjustedPrices(amount, getBidDepth());
    }

    @Override
    public double buyAmount(String buy, String buyWith, double buyWithAmount) {
        List<Pair<Double, Double>> list = getPairAskDepth().get(Pair.of(buyWith, buy));
        return ExchangeApiUtils.getUnitsForAmount(buyWithAmount, list);
    }

    @Override
    public double buyUnits(String buy, String buyWith, double buyUnits) {
        List<Pair<Double, Double>> list = getPairAskDepth().get(Pair.of(buyWith, buy));
        return ExchangeApiUtils.getAmountForUnits(buyUnits, list);
    }

    @Override
    public double sellAmount(String sell, String sellFor, double sellForAmount) {
        List<Pair<Double, Double>> list = getPairBidDepth().get(Pair.of(sellFor, sell));
        return ExchangeApiUtils.getUnitsForAmount(sellForAmount, list);
    }

    @Override
    public double sellUnits(String sell, String sellFor, double sellUnits) {
        List<Pair<Double, Double>> list = getPairBidDepth().get(Pair.of(sellFor, sell));
        return ExchangeApiUtils.getAmountForUnits(sellUnits, list);
    }
}
