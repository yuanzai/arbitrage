package com.arbitrage;

import java.io.IOException;
import java.util.Map;

/**
 * Created by junyuanlau on 4/7/17.
 */
interface ExchangeApi {
    Map<String, Double> getLastPrices() throws IOException;
    Map<String, Double> getBidPrices();
    Map<String, Double> getAskPrices();
}
