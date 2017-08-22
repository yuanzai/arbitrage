package com.arbitrage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by junyuanlau on 1/7/17.
 */
public class EmailClient {
    private static final Logger logger = LoggerFactory.getLogger(EmailClient.class);
    public static final String EMAIL_PROPERTIES = "email.properties";

    private EmailClient() {}

    private static String emailBodyPrices(Map<String, Double> coinChinaPrice, Map<String, Double> usCoinPrice, String diffResult) {
        StringBuilder currentInfo = new StringBuilder();
        currentInfo.append("BTC: China-").append(coinChinaPrice.get(CouponPullerTask.BTC)).append(" US-").append(usCoinPrice.get(CouponPullerTask.BTC)).append("\n");
        currentInfo.append("LTC: China-").append(coinChinaPrice.get(CouponPullerTask.LTC)).append(" US-").append(usCoinPrice.get(CouponPullerTask.LTC)).append("\n");
        currentInfo.append("ETH: China-").append(coinChinaPrice.get(CouponPullerTask.ETH)).append(" US-").append(usCoinPrice.get(CouponPullerTask.ETH)).append("\n");
        currentInfo.append("XRP: China-").append(coinChinaPrice.get(CouponPullerTask.XRP)).append(" US-").append(usCoinPrice.get(CouponPullerTask.XRP)).append("\n");
        return currentInfo.toString();
    }

    private static void sendEmail(String emailBody, InternetAddress[] recipients) throws IOException {
        Properties props =  new Properties();
        props.load(new FileInputStream(EMAIL_PROPERTIES));

        String host = props.getProperty("mail.smtp.host");
        String user = props.getProperty("mail.smtp.user");
        String password = props.getProperty("mail.smtp.password");
        String port = props.getProperty("mail.smtp.port");

        Session session = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(session);

        logger.debug("Host: {} Port: {} User: {}", host, port, user);

        // Create the email addresses involved
        try {
            InternetAddress from = new InternetAddress(user);
            message.setSubject("Close price alarm");
            message.setFrom(from);
            message.addRecipients(Message.RecipientType.TO, recipients);

            // Create a multi-part to combine the parts
            Multipart multipart = new MimeMultipart("alternative");

            // Create your text message part
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("some text to send");

            // Add the text part to the multipart
            multipart.addBodyPart(messageBodyPart);

            // Create the html part
            messageBodyPart = new MimeBodyPart();
            String htmlMessage = emailBody;
            messageBodyPart.setContent(htmlMessage, "text/html");

            // Add html part to multi part
            multipart.addBodyPart(messageBodyPart);

            // Associate multi-part with message
            message.setContent(multipart);

            // Send message
            Transport transport = session.getTransport("smtp");
            transport.connect(host, user, password);
            transport.sendMessage(message, message.getAllRecipients());
            logger.debug("Email successfully sent to {}", recipients.toString());
        } catch (MessagingException e) {
            logger.error("?? {}", e);
        }
    }


}
