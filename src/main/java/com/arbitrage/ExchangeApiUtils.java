package com.arbitrage;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Created by junyuanlau on 20/8/17.
 */
public class ExchangeApiUtils {
    private ExchangeApiUtils(){};

    static String httpGetResponse(CloseableHttpClient httpClient, String url) throws IOException {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("accept", "*/*");
        CloseableHttpResponse response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String json = EntityUtils.toString(entity);
        response.close();
        return json;
    }

}
