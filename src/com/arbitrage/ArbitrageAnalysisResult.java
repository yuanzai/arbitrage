package com.arbitrage;

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

}

