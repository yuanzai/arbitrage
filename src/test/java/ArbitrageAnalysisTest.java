import com.arbitrage.ArbitrageAnalysis;
import com.arbitrage.ArbitrageAnalysisResult;
import com.arbitrage.Trade;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

/**
 * Created by junyuanlau on 1/7/17.
 */
public class ArbitrageAnalysisTest {

    @Test
    public void testTwoCoinArbritage() {
        // Overpriced Market 1
        Map<String, Double> pricesHigher = ImmutableMap.<String, Double>builder()
                .put("coin1", 100d) // buy
                .put("coin2", 1000d) // sell
                .build();

        Map<String, Double> pricesLower = ImmutableMap.<String, Double>builder()
                .put("coin1", 110d) // sell
                .put("coin2", 1050d) // buy
                .build();

        ArbitrageAnalysis analysis = new ArbitrageAnalysis("market1", pricesHigher, "market2", pricesLower);
        ArbitrageAnalysisResult result = analysis.getResult();
        assertThat(result, notNullValue());
        Trade trade1 = result.getTrade1();
        Trade trade2 = result.getTrade2();
        assertThat(trade1, notNullValue());
        assertThat(trade2, notNullValue());
        assertThat(trade1.getMarket(), is("market1"));
        assertThat(trade1.getBuy(), is("coin1"));
        assertThat(trade1.getSell(), is("coin2"));
        assertThat(trade1.getBuyPrice(), is(100d));
        assertThat(trade1.getSellPrice(), is(1000d));

        assertThat(trade2.getMarket(), is("market2"));
        assertThat(trade2.getBuy(), is("coin2"));
        assertThat(trade2.getSell(), is("coin1"));
        assertThat(trade2.getBuyPrice(), is(1050d));
        assertThat(trade2.getSellPrice(), is(110d));

        // Overpriced Market 2
        analysis = new ArbitrageAnalysis("market1", pricesLower, "market2", pricesHigher);
        result = analysis.getResult();
        trade1 = result.getTrade1();
        trade2 = result.getTrade2();
        assertThat(trade1, notNullValue());
        assertThat(trade2, notNullValue());
        assertThat(trade1.getMarket(), is("market1"));
        assertThat(trade1.getBuy(), is("coin2"));
        assertThat(trade1.getSell(), is("coin1"));
        assertThat(trade1.getBuyPrice(), is(1050d));
        assertThat(trade1.getSellPrice(), is(110d));

        assertThat(trade2.getMarket(), is("market2"));
        assertThat(trade2.getBuy(), is("coin1"));
        assertThat(trade2.getSell(), is("coin2"));
        assertThat(trade2.getBuyPrice(), is(100d));
        assertThat(trade2.getSellPrice(), is(1000d));
    }

    @Test
    public void testThreeCoinArbritage() {
        Map<String, Double> pricesHigher = ImmutableMap.<String, Double>builder()
                .put("coin1", 100d) // buy
                .put("coin2", 1000d) //
                .put("coin3", 10000d) // sell
                .build();

        Map<String, Double> pricesLower = ImmutableMap.<String, Double>builder()
                .put("coin1", 110d) // sell
                .put("coin2", 1050d) //
                .put("coin3", 10001d) // buy
                .build();

        ArbitrageAnalysis analysis = new ArbitrageAnalysis("market1", pricesHigher, "market2", pricesLower);
        ArbitrageAnalysisResult result = analysis.getResult();
        assertThat(result, notNullValue());
        Trade trade1 = result.getTrade1();
        Trade trade2 = result.getTrade2();
        assertThat(trade1, notNullValue());
        assertThat(trade2, notNullValue());
        assertThat(trade1.getMarket(), is("market1"));
        assertThat(trade1.getBuy(), is("coin1"));
        assertThat(trade1.getSell(), is("coin3"));
        assertThat(trade1.getBuyPrice(), is(100d));
        assertThat(trade1.getSellPrice(), is(10000d));

        assertThat(trade2.getMarket(), is("market2"));
        assertThat(trade2.getBuy(), is("coin3"));
        assertThat(trade2.getSell(), is("coin1"));
        assertThat(trade2.getBuyPrice(), is(10001d));
        assertThat(trade2.getSellPrice(), is(110d));

        analysis = new ArbitrageAnalysis("market1", pricesLower, "market2", pricesHigher);
        result = analysis.getResult();
        trade1 = result.getTrade1();
        trade2 = result.getTrade2();
        assertThat(trade1, notNullValue());
        assertThat(trade2, notNullValue());
        assertThat(trade1.getMarket(), is("market1"));
        assertThat(trade1.getBuy(), is("coin3"));
        assertThat(trade1.getSell(), is("coin1"));
        assertThat(trade1.getBuyPrice(), is(10001d));
        assertThat(trade1.getSellPrice(), is(110d));

        assertThat(trade2.getMarket(), is("market2"));
        assertThat(trade2.getBuy(), is("coin1"));
        assertThat(trade2.getSell(), is("coin3"));
        assertThat(trade2.getBuyPrice(), is(100d));
        assertThat(trade2.getSellPrice(), is(10000d));
    }

    @Test
    public void testTwoCoinArbritageWithBidAsk() {
        // Overpriced Market 1
        Map<String, Double> pricesHigherBid = ImmutableMap.<String, Double>builder()
                .put("coin1", 99d)
                .put("coin2", 990d) // sell
                .build();

        Map<String, Double> pricesHigherAsk = ImmutableMap.<String, Double>builder()
                .put("coin1", 101d) // buy
                .put("coin2", 1010d)
                .build();


        Map<String, Double> pricesLowerBid = ImmutableMap.<String, Double>builder()
                .put("coin1", 109d) // sell
                .put("coin2", 1040d)
                .build();

        Map<String, Double> pricesLowerAsk = ImmutableMap.<String, Double>builder()
                .put("coin1", 111d)
                .put("coin2", 1060d) // buy
                .build();


        ArbitrageAnalysis analysis = new ArbitrageAnalysis("market1", pricesHigherBid, pricesHigherAsk, "market2", pricesLowerBid, pricesLowerAsk);
        ArbitrageAnalysisResult result = analysis.getResult();
        assertThat(result, notNullValue());
        Trade trade1 = result.getTrade1();
        Trade trade2 = result.getTrade2();
        assertThat(trade1, notNullValue());
        assertThat(trade2, notNullValue());
        assertThat(trade1.getMarket(), is("market1"));
        assertThat(trade1.getBuy(), is("coin1"));
        assertThat(trade1.getSell(), is("coin2"));
        assertThat(trade1.getBuyPrice(), is(101d));
        assertThat(trade1.getSellPrice(), is(990d));

        assertThat(trade2.getMarket(), is("market2"));
        assertThat(trade2.getBuy(), is("coin2"));
        assertThat(trade2.getSell(), is("coin1"));
        assertThat(trade2.getBuyPrice(), is(1060d));
        assertThat(trade2.getSellPrice(), is(109d));
    }

    @Test
    public void testThreeCoinArbritageWithBidAsk() {
        Map<String, Double> pricesHigherBid = ImmutableMap.<String, Double>builder()
                .put("coin1", 99d)
                .put("coin2", 990d)
                .put("coin3", 9900d) // sell
                .build();

        Map<String, Double> pricesHigherAsk = ImmutableMap.<String, Double>builder()
                .put("coin1", 101d) // buy
                .put("coin2", 1010d)
                .put("coin3", 10100d)
                .build();

        Map<String, Double> pricesLowerBid = ImmutableMap.<String, Double>builder()
                .put("coin1", 109d) // sell
                .put("coin2", 1040d)
                .put("coin3", 10009d)
                .build();

        Map<String, Double> pricesLowerAsk = ImmutableMap.<String, Double>builder()
                .put("coin1", 111d)
                .put("coin2", 1060d)
                .put("coin3", 10011d) // buy
                .build();

        ArbitrageAnalysis analysis = new ArbitrageAnalysis("market1", pricesHigherBid, pricesHigherAsk, "market2", pricesLowerBid, pricesLowerAsk);
        ArbitrageAnalysisResult result = analysis.getResult();
        assertThat(result, notNullValue());
        Trade trade1 = result.getTrade1();
        Trade trade2 = result.getTrade2();
        assertThat(trade1, notNullValue());
        assertThat(trade2, notNullValue());
        assertThat(trade1.getMarket(), is("market1"));
        assertThat(trade1.getBuy(), is("coin1"));
        assertThat(trade1.getSell(), is("coin3"));
        assertThat(trade1.getBuyPrice(), is(101d));
        assertThat(trade1.getSellPrice(), is(9900d));

        assertThat(trade2.getMarket(), is("market2"));
        assertThat(trade2.getBuy(), is("coin3"));
        assertThat(trade2.getSell(), is("coin1"));
        assertThat(trade2.getBuyPrice(), is(10011d));
        assertThat(trade2.getSellPrice(), is(109d));
    }

}