package com.arbitrage;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by junyuanlau on 1/7/17.
 */
public class ArbitrageAnalysis {
    private static final Logger logger = LoggerFactory.getLogger(CouponPullerTask.class);

    private String market1;
    private Map<String, Double> marketBidPrices1; // Sell at this price
    private Map<String, Double> marketAskPrices1; // Buy at this price
    private String market2;
    private Map<String, Double> marketBidPrices2; // Sell at this price
    private Map<String, Double> marketAskPrices2; // Buy at this price

    public ArbitrageAnalysis(String market1,
                             Map<String, Double> marketBidPrices1,
                             Map<String, Double> marketAskPrices1,
                             String market2,
                             Map<String, Double> marketBidPrices2,
                             Map<String, Double> marketAskPrices2) {
        this.market1 = market1;
        this.marketBidPrices1 = marketBidPrices1;
        this.marketAskPrices1 = marketAskPrices1;
        this.market2 = market2;
        this.marketBidPrices2 = marketBidPrices2;
        this.marketAskPrices2 = marketAskPrices2;
        throw new UnsupportedOperationException("Not unit tested for bid ask yet");
    }

    public ArbitrageAnalysis(String market1,
                             Map<String, Double> marketPrices1,
                             String market2,
                             Map<String, Double> marketPrices2) {
        this.market1 = market1;
        this.marketBidPrices1 = marketPrices1;
        this.marketAskPrices1 = marketPrices1;
        this.market2 = market2;
        this.marketBidPrices2 = marketPrices2;
        this.marketAskPrices2 = marketPrices2;
    }

    public ArbitrageAnalysisResult getResult(){
        NavigableMap<String, Double> market1BuyMarket2Sell = diffAnalysis(marketBidPrices2, marketAskPrices1);
        NavigableMap<String, Double> market2BuyMarket1Sell = diffAnalysis(marketAskPrices2, marketBidPrices1);
        Set<String> intersection = Sets.intersection(new HashSet<>(market1BuyMarket2Sell.keySet()), new HashSet<>(market2BuyMarket1Sell.keySet()));

        if (intersection.size() < 2) {
            logger.debug("Less than 2 coin prices available from both markets");
            return new ArbitrageAnalysisResult(null, null, 0d);
        }

        Map.Entry<String, Double> market1BuyFirstEntry = market1BuyMarket2Sell.firstEntry();
        Map.Entry<String, Double> market2BuyLastEntry = market2BuyMarket1Sell.lastEntry();

        Map.Entry<String, Double> market2BuyFirstEntry = market2BuyMarket1Sell.firstEntry();
        Map.Entry<String, Double> market1BuyLastEntry = market1BuyMarket2Sell.lastEntry();

        double market1BuyCheapArbitrageRatio = getArbitrageRatioFromEntries(market1BuyFirstEntry, market2BuyLastEntry);
        double market2BuyCheapArbitrageRatio = getArbitrageRatioFromEntries(market2BuyFirstEntry, market1BuyLastEntry);

        String market1BuyMarket2SellCoin;
        String market2BuyMarket1SellCoin;
        double arbitrage;
        if (market1BuyCheapArbitrageRatio > market2BuyCheapArbitrageRatio) {
            market1BuyMarket2SellCoin = market2BuyLastEntry.getKey();
            market2BuyMarket1SellCoin = market1BuyFirstEntry.getKey();
            arbitrage = market1BuyCheapArbitrageRatio;
        } else {
            market1BuyMarket2SellCoin = market1BuyLastEntry.getKey();
            market2BuyMarket1SellCoin = market2BuyFirstEntry.getKey();
            arbitrage = market2BuyCheapArbitrageRatio;
        }
        Trade trade1 = new Trade(market1, market1BuyMarket2SellCoin, market2BuyMarket1SellCoin, marketAskPrices1.get(market1BuyMarket2SellCoin), marketBidPrices1.get(market2BuyMarket1SellCoin));
        Trade trade2 = new Trade(market2, market2BuyMarket1SellCoin, market1BuyMarket2SellCoin, marketAskPrices2.get(market2BuyMarket1SellCoin), marketBidPrices2.get(market1BuyMarket2SellCoin));
        return new ArbitrageAnalysisResult(trade1, trade2, arbitrage);
    }

    public double getArbitrageRatioFromEntries(Map.Entry<String, Double> cheapEntry, Map.Entry<String, Double> expensiveEntry) {
        double firstValue = cheapEntry.getValue();
        double lastValue = expensiveEntry.getValue();
        return lastValue / firstValue;
    }

    static NavigableMap<String, Double> diffAnalysis(Map<String, Double> bidPrices, Map<String, Double> askPrices) {
        Set<String> coins = new HashSet<>(bidPrices.keySet());
        coins.addAll(askPrices.keySet());
        Map<String, Double> diffPercentage = new HashMap<>();
        for (String coin : coins) {
            Double coin1 = bidPrices.get(coin);
            Double coin2 = askPrices.get(coin);
            if (coin1 != null && coin2 != null) {
                diffPercentage.put(coin, coin1 / coin2);
            } else {
                diffPercentage.put(coin, 0d);
            }
        }
        return sortByValues(diffPercentage);
    }

    public static <K, V extends Comparable<V>> NavigableMap<K, V>
    sortByValues(final Map<K, V> map) {
        Comparator<K> valueComparator =
                new Comparator<K>() {
                    public int compare(K k1, K k2) {
                        int compare =
                                map.get(k1).compareTo(map.get(k2));
                        if (compare == 0)
                            return 1;
                        else
                            return compare;
                    }
                };
        NavigableMap<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
        sortedByValues.putAll(map);
        return sortedByValues;
    }

}
