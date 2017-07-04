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

}