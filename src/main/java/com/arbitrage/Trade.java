package com.arbitrage;

import com.google.common.base.Joiner;

/**
 * Created by junyuanlau on 1/7/17.
 */
public class Trade {
    private String market;
    private String buy;
    private String sell;
    private double buyPrice;
    private double sellPrice;

    public Trade(String market, String buy, String sell, double buyPrice, double sellPrice) {
        this.market = market;
        this.buy = buy;
        this.sell = sell;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    public String getMarket() {
        return market;
    }

    public String getBuy() {
        return buy;
    }

    public String getSell() {
        return sell;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public String toCsvString() {
        return Joiner.on(",").join(market, buy, buyPrice, sell, sellPrice);
    }

    public String toLogString() {
        return String.format("Exchange %s\nBuy %s @ %f\nSell %s @ %f", market, buy, buyPrice, sell, sellPrice);
    }
}