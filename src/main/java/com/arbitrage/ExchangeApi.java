package com.arbitrage;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Map;

/**
 * Created by junyuanlau on 4/7/17.
 */
interface ExchangeApi {
    Map<String, Double> getAskPrices();
    Map<String, Double> getBidPrices();
    Map<String, Double> getLastPrices();

    Map<String, Double> getAskBtcPrices();
    Map<String, Double> getBidBtcPrices();
    Map<String, Double> getLastBtcPrices();

    /**
     * Pair of ask price to size. List should be sorted ascending
     */
    ListMultimap<String, Pair<Double, Double>> getAskDepth();

    /**
     * Pair of bid price to size. List should be sorted descending
     */
    ListMultimap<String, Pair<Double, Double>> getBidDepth();

    Map<String, Double> getAskDepthAdjustedPrices(double amount);
    Map<String, Double> getBidDepthAdjustedPrices(double amount);

    ListMultimap<Pair<String, String>, Pair<Double, Double>> getPairAskDepth();
    ListMultimap<Pair<String, String>, Pair<Double, Double>> getPairBidDepth();


}
