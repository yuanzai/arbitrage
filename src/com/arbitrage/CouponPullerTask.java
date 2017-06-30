/**
 * Created by junyuanlau on 30/6/17.
 */

package com.arbitrage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.conn.util.DomainType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.resources.Messages_pt_BR;

public class CouponPullerTask {
    private static final Logger logger = LoggerFactory.getLogger(CouponPullerTask.class);
    private static final String BTC = "btc";
    private static final String LTC = "ltc";
    private static final String ETH = "eth";
    private static final String XRP = "xrp";
    private static final String BTS = "bts";
    private static final String JUBI_BASE_URL = "https://www.jubi.com/api/v1/ticker?coin=";
    private static final String POLONIEX_URL = "https://poloniex.com/public?command=returnTicker";
    private static final String LAST = "last";

    private static final Map<String, String> coinToApi = ImmutableMap.<String, String>builder()
            .put(BTC, JUBI_BASE_URL + "btc")
            .put(LTC, JUBI_BASE_URL + "ltc")
            .put(ETH, JUBI_BASE_URL + "eth")
            .put(XRP, JUBI_BASE_URL + "xrp")
            .put(BTS, JUBI_BASE_URL + "bts")
            .build();

    private static final Map<String, Double> diff = ImmutableMap.<String, Double>builder()
            .put(BTC, 300d)
            .put(LTC, 15d)
            .put(ETH, 9d)
            .put(XRP, .06)
            .put(BTS, .06)
            .build();

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) throws Exception {
        scheduler.scheduleAtFixedRate(new TaskRunnable(), 0, 60, TimeUnit.SECONDS);
    }

    private void handleData() throws Exception {
        Map<String, Double> coinChinaPrice = getChinaPrices();
        logger.debug("China coin price : {}",coinChinaPrice.toString());
        //double cnyPrice = getCnyPrice();
        double rmb = getRmbPrice();
        Map<String, Double> usCoinPrice = getUsCoinPrice(rmb);
        String diffResult = getDiffResult(coinChinaPrice, rmb, usCoinPrice);
        logger.debug("US coin price : {}", usCoinPrice.toString());
        Map<String, Double> diffAnalysis = diffAnalysis(coinChinaPrice, usCoinPrice);
        logger.debug("Percentage diff (china / us) : {}", diffAnalysis.toString());
        largestArbitrage(diffAnalysis);

        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        if (minute == 10 || !diffResult.isEmpty()) {
            //sendEmail(coinChinaPrice, usCoinPrice, diffResult);
        }
    }

    private Map<String, Double> diffAnalysis(Map<String, Double> coinChinaPrice, Map<String, Double> usCoinPrice) {
        Map<String, Double> diffPercentage = new HashMap<>();
        for (String name : diff.keySet()) {
            Double china = coinChinaPrice.get(name);
            Double us = usCoinPrice.get(name);
            if (china != null && us != null) {
                diffPercentage.put(name, china / us);
            } else {
                diffPercentage.put(name, 0d);
            }
        }
        return diffPercentage;
    }

    private void largestArbitrage(Map<String, Double> diffAnalysis) {
        TreeMap<String, Double> sortedMap = diffAnalysis.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue())
                .filter(entry -> entry.getValue() != 0d)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (e1, e2) -> e1, TreeMap::new));
        if (sortedMap.size() < 2) {
            return;
        }
        Map.Entry<String, Double> firstEntry = sortedMap.firstEntry();
        Map.Entry<String, Double> lastEntry = sortedMap.lastEntry();

        double firstValue = firstEntry.getValue();
        double lastValue = lastEntry.getValue();
        double arbitrage = lastValue / firstValue;
        logger.debug("Arbitrage {} - buy: {} sell: {}", arbitrage, firstEntry.getKey(), lastEntry.getKey());
    }

    private String getDiffResult(Map<String, Double> coinChinaPrice, double rmb, Map<String, Double> usCoinPrice) {
        StringBuilder sb = new StringBuilder();
        for(String name : diff.keySet()) {
            double china = coinChinaPrice.get(name);
            double us = usCoinPrice.get(name);
            if (Math.abs(china - us) <= diff.get(name)) {
                sb.append("price is close： " + name + " China: " + china + " US: " + us + " RMB: " + rmb + "\n\n");
            }
        }
        return sb.toString();
    }

    private Map<String, Double> getUsCoinPrice(double rmb) throws IOException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        Map<String, Double> usCoinPrice = new HashMap<>();
        CloseableHttpClient httpClient = builder.build();
        HttpGet httpGet = new HttpGet(POLONIEX_URL);
        CloseableHttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity);
        response.close();
        httpClient.close();

        JSONObject jsonObject = new JSONObject(result);

        double btc = jsonObject.getJSONObject("USDT_BTC").getDouble(LAST);
        double ltc = jsonObject.getJSONObject("USDT_LTC").getDouble(LAST);
        double eth = jsonObject.getJSONObject("USDT_ETH").getDouble(LAST);
        double xrp = jsonObject.getJSONObject("USDT_XRP").getDouble(LAST);
        double bts = jsonObject.getJSONObject("BTC_BTS").getDouble(LAST) * btc;

        usCoinPrice.put(BTC, btc*rmb);
        usCoinPrice.put(LTC, ltc*rmb);
        usCoinPrice.put(ETH, eth*rmb);
        usCoinPrice.put(XRP, xrp*rmb);
        usCoinPrice.put(BTS, bts*rmb);
        return usCoinPrice;
    }

    private void sendEmail(Map<String, Double> coinChinaPrice, Map<String, Double> usCoinPrice, String diffResult) {
        StringBuilder currentInfo = new StringBuilder();
        currentInfo.append("BTC: China-").append(coinChinaPrice.get(BTC)).append(" US-").append(usCoinPrice.get(BTC)).append("\n");
        currentInfo.append("LTC: China-").append(coinChinaPrice.get(LTC)).append(" US-").append(usCoinPrice.get(LTC)).append("\n");
        currentInfo.append("ETH: China-").append(coinChinaPrice.get(ETH)).append(" US-").append(usCoinPrice.get(ETH)).append("\n");
        currentInfo.append("XRP: China-").append(coinChinaPrice.get(XRP)).append(" US-").append(usCoinPrice.get(XRP)).append("\n");
        currentInfo.append("BTS: China-").append(coinChinaPrice.get(BTS)).append(" US-").append(usCoinPrice.get(BTS)).append("\n");

        Properties props = System.getProperties();
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.user", "bitcoininfoalarm");
        props.put("mail.smtp.password", "makemoney!");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", true);

        Session session = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(session);

        System.out.println("Port: " + session.getProperty("mail.smtp.port"));

        // Create the email addresses involved
        try {
            InternetAddress from = new InternetAddress("bitcoininfoalarm");
            message.setSubject("Close price alarm");
            message.setFrom(from);
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse("verafeng1129@163.com"));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse("bitcoininfoalarm@gmail.com"));

            // Create a multi-part to combine the parts
            Multipart multipart = new MimeMultipart("alternative");

            // Create your text message part
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("some text to send");

            // Add the text part to the multipart
            multipart.addBodyPart(messageBodyPart);

            // Create the html part
            messageBodyPart = new MimeBodyPart();
            String htmlMessage = diffResult.isEmpty() ? currentInfo.toString() : diffResult;
            messageBodyPart.setContent(htmlMessage, "text/html");


            // Add html part to multi part
            multipart.addBodyPart(messageBodyPart);

            // Associate multi-part with message
            message.setContent(multipart);

            // Send message
            Transport transport = session.getTransport("smtp");
            transport.connect("smtp.gmail.com", "bitcoininfoalarm", "makemoney!");
            transport.sendMessage(message, message.getAllRecipients());


        } catch (AddressException e) {
            logger.error("?? {}", e);
        } catch (MessagingException e) {
            logger.error("?? {}", e);
        }
    }

    Map<String, Double> getChinaPrices() throws IOException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient httpClient = builder.build();
        //http://api.btc38.com/v1/ticker.php?c=all&mk_type=cny
        //https://www.jubi.com/api/v1/ticker?coin=btc

        Map<String, Double> coinChinaPrice = new HashMap<>();

        for (Map.Entry<String, String> entry : coinToApi.entrySet()) {
            HttpGet httpGet = new HttpGet(entry.getValue());
            httpGet.setHeader("accept", "*/*");
            CloseableHttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            JSONObject jsonObject2 = new JSONObject(result);
            coinChinaPrice.put(entry.getKey(), jsonObject2.getDouble("buy"));
            response.close();
        }
        httpClient.close();
        return coinChinaPrice;
    }

    double getCnyPrice() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient testClient = builder.build();
        CloseableHttpResponse testResponse = null;
        HttpGet testGet = new HttpGet("http://api.btc38.com/v1/ticker.php?c=all&mk_type=cny");
        testGet.setHeader("accept", "*/*");
        JSONObject cnyObject = null;
        try {
            testResponse = testClient.execute(testGet);
            HttpEntity testEntity = testResponse.getEntity();
            String testResult = EntityUtils.toString(testEntity);
            cnyObject = new JSONObject(testResult);
            testResponse.close();
            testClient.close();

        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return cnyObject.getDouble("somestring");
    }

    double getRmbPrice() throws IOException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient exchangeRateHttpClient = builder.build();
        HttpGet exchangeGet = new HttpGet("http://api.fixer.io/latest?base=USD");
        exchangeGet.setHeader("accept", "*/*");
        CloseableHttpResponse exchangeResponse = exchangeRateHttpClient.execute(exchangeGet);
        HttpEntity exchangeEntity = exchangeResponse.getEntity();
        String exchangeResult = EntityUtils.toString(exchangeEntity);
        JSONObject jsonObject = new JSONObject(exchangeResult);
        JSONObject rates = jsonObject.getJSONObject("rates");
        exchangeResponse.close();
        exchangeRateHttpClient.close();
        return rates.getDouble("CNY");
    }

    public static class TaskRunnable implements Runnable {

        public void run() {
            logger.debug("Run task");
            CouponPullerTask task = new CouponPullerTask();
            try {
                task.handleData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static void main(String args[]) {
            (new Thread(new TaskRunnable())).start();
        }

    }
}



