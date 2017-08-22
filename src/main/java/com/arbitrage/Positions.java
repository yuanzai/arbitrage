package com.arbitrage;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by junyuanlau on 21/8/17.
 */
public class Positions {

    private Map<String, Double> positions = new HashMap<>();

    public Map<String, Double> getPositions() {
        return positions;
    }

    double buyUnits(Exchange exchange, String buy, String buyWith, double units) {
        double amount = exchange.buyUnits(buy, buyWith, units);
        positions.merge(buyWith, -amount, (v1, v2) -> v1 + v2);
        positions.merge(buy, units, (v1, v2) -> v1 + v2);
        return amount;
    }

    double sellUnits(Exchange exchange, String sell, String sellFor, double units) {
        double amount = exchange.sellUnits(sell, sellFor, units);
        positions.merge(sellFor, amount, (v1, v2) -> v1 + v2);
        positions.merge(sell, -units, (v1, v2) -> v1 + v2);
        return amount;
    }

    double buyAmount(Exchange exchange, String buy, String buyWith, double amount) {
        double units = exchange.buyAmount(buy, buyWith, amount);
        positions.merge(buy, units, (v1, v2) -> v1 + v2);
        positions.merge(buyWith, -amount, (v1, v2) -> v1 + v2);
        return units;
    }

    double sellAmount(Exchange exchange, String sell, String sellFor, double amount) {
        double units = exchange.sellAmount(sell, sellFor, amount);
        positions.merge(sell, -units, (v1, v2) -> v1 + v2);
        positions.merge(sellFor, amount, (v1, v2) -> v1 + v2);
        return units;
    }

}