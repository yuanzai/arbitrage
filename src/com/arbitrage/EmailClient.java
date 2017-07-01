package com.arbitrage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Map;
import java.util.Properties;

/**
 * Created by junyuanlau on 1/7/17.
 */
public class EmailClient {
    private static final Logger logger = LoggerFactory.getLogger(EmailClient.class);

    private static void sendEmail(Map<String, Double> coinChinaPrice, Map<String, Double> usCoinPrice, String diffResult) {
        StringBuilder currentInfo = new StringBuilder();
        currentInfo.append("BTC: China-").append(coinChinaPrice.get(CouponPullerTask.BTC)).append(" US-").append(usCoinPrice.get(CouponPullerTask.BTC)).append("\n");
        currentInfo.append("LTC: China-").append(coinChinaPrice.get(CouponPullerTask.LTC)).append(" US-").append(usCoinPrice.get(CouponPullerTask.LTC)).append("\n");
        currentInfo.append("ETH: China-").append(coinChinaPrice.get(CouponPullerTask.ETH)).append(" US-").append(usCoinPrice.get(CouponPullerTask.ETH)).append("\n");
        currentInfo.append("XRP: China-").append(coinChinaPrice.get(CouponPullerTask.XRP)).append(" US-").append(usCoinPrice.get(CouponPullerTask.XRP)).append("\n");
        currentInfo.append("BTS: China-").append(coinChinaPrice.get(CouponPullerTask.BTS)).append(" US-").append(usCoinPrice.get(CouponPullerTask.BTS)).append("\n");

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


        } catch (MessagingException e) {
            logger.error("?? {}", e);
        }
    }
}
