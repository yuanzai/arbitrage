import com.arbitrage.ArbitrageAnalysis;
import com.arbitrage.ArbitrageAnalysisResult;
import com.arbitrage.Trade;
import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Created by junyuanlau on 1/7/17.
 */
public class ArbitrageAnalysisTest {

    @Test
    public void testTwoCoinArbritage(){
        // Overpriced Market 1
        Map<String, Double> pricesHigher = ImmutableMap.<String, Double>builder()
                .put("coin1", 100d)
                .put("coin2", 1000d)
                .build();

        Map<String, Double> pricesLower = ImmutableMap.<String, Double>builder()
                .put("coin1", 110d)
                .put("coin2", 1050d)
                .build();

        ArbitrageAnalysis analysis = new ArbitrageAnalysis("market1", pricesHigher, pricesHigher, "market2", pricesLower, pricesLower);
        ArbitrageAnalysisResult result = analysis.getResult();
        Trade trade1 = result.getTrade1();
        Trade trade2 = result.getTrade2();
        assertThat(trade1.getBuy(), is("coin1"));
        assertThat(trade1.getSell(), is("coin2"));
        assertThat(trade1.getBuyPrice(), is(100d));
        assertThat(trade1.getSellPrice(), is(1000d));

        // Overpriced Market 2
        analysis = new ArbitrageAnalysis("market1", pricesLower, pricesLower, "market2", pricesHigher, pricesHigher);
        result = analysis.getResult();
        trade1 = result.getTrade1();
        assertThat(trade1.getBuy(), is("coin1"));



    }

}
