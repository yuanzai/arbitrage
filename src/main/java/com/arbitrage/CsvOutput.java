package com.arbitrage;

import com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by junyuanlau on 1/7/17.
 */
public class CsvOutput {

    private static final Logger logger = LoggerFactory.getLogger(CsvOutput.class);

    private CsvOutput(){}

    static void appendRowToFile(String path, String row) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            logger.debug("Creating file with headers at {}", path);
            FileWriter fileWriter = new FileWriter(path, true);
            fileWriter.append(getCsvHeader() + "\n");
            fileWriter.flush();
            fileWriter.close();
        }
        logger.debug("Append to {}", path);
        FileWriter fileWriter = new FileWriter(path, true);
        fileWriter.append(row + "\n");
        fileWriter.flush();
        fileWriter.close();
    }

    static String getCsvHeader() {
        List<String> headers = new ArrayList<>();
        headers.add("UTCDateTime");
        headers.add("Market1");
        headers.add("Buy1");
        headers.add("BuyPrice1");
        headers.add("Sell1");
        headers.add("SellPrice1");

        headers.add("Market2");
        headers.add("Buy2");
        headers.add("BuyPrice2");
        headers.add("Sell2");
        headers.add("SellPrice2");

        headers.add("ArbitrageRatio");

        for (String coin : CouponPullerTask.COINS) {
            headers.add(coin + "_CN");
            headers.add(coin + "_US");
        }
        return Joiner.on(",").join(headers);
    }

    static String getCsvRow(ArbitrageAnalysisResult result, Map<String, Double> chinaCoinPrice, Map<String, Double> usCoinPrice) {
        List<String> prices = new ArrayList<>();
        for (String coin : CouponPullerTask.COINS) {
            Double cnPrice = chinaCoinPrice.get(coin);
            Double usPrice = usCoinPrice.get(coin);
            if (cnPrice != null) {
                prices.add(cnPrice.toString());
            } else {
                prices.add("");
            }
            if (usPrice != null) {
                prices.add(usPrice.toString());
            } else {
                prices.add("");
            }
        }
        LocalDateTime dateTime = LocalDateTime.now(Clock.systemUTC());
        return Joiner.on(",").join(dateTime, result, Joiner.on(",").join(prices));
    }
}
