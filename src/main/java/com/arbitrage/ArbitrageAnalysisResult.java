package com.arbitrage;

import com.google.common.base.Joiner;

/**
 * Created by junyuanlau on 1/7/17.
 */
public class ArbitrageAnalysisResult {
    private Trade trade1;
    private Trade trade2;
    private double arbitrageReturn;

    public ArbitrageAnalysisResult(Trade trade1, Trade trade2, double arbitrageReturn) {
        this.trade1 = trade1;
        this.trade2 = trade2;
        this.arbitrageReturn = arbitrageReturn;
    }

    public Trade getTrade1() {
        return trade1;
    }

    public Trade getTrade2() {
        return trade2;
    }

    public double getArbitrageReturn() {
        return arbitrageReturn;
    }

    public boolean validate() {
        boolean validateResult = true;
        validateResult &= !trade1.getMarket().equals(trade2.getMarket());
        validateResult &= trade1.getBuy().equals(trade2.getSell());
        validateResult &= trade2.getBuy().equals(trade1.getSell());
        return validateResult;
    }

    @Override
    public String toString() {
        return Joiner.on(",").join(trade1.toCsvString(), trade2.toCsvString(), arbitrageReturn);
    }
}

