package com.arbitrage;

/**
 * Created by junyuanlau on 21/8/17.
 */
public interface Exchange {
    double buyAmount(String buy, String buyWith, double buyWithAmount);
    double buyUnits(String buy, String buyWith, double buyUnits);

    double sellAmount(String sell, String sellFor, double sellForAmount);
    double sellUnits(String sell, String sellFor, double sellUnits);
}
