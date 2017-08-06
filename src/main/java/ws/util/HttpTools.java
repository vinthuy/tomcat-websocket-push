package ws.util;


import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ruiyong.hry on 29/06/2017.
 */
public class HttpTools {
//
//    private final static  String applicationType = "application/x-www-form-urlencoded";

    private static final String utf8 = "UTF-8";

    private static final Logger logger = LoggerFactory.getLogger(HttpTools.class);


    public static byte[] postByte(String url, HttpClient client, byte[] requestBytes) {
        return postByte(url, client, requestBytes, null);
    }

    // send bytes and recv bytes
    public static byte[] postByte(String url, HttpClient client, byte[] requestBytes, String contentType) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new ByteArrayEntity(requestBytes));
        if (contentType != null)
            httpPost.setHeader("Content-type", contentType);
        HttpResponse httpResponse = null;
        try {
            httpResponse = client.execute(httpPost);
            int status = httpResponse.getStatusLine().getStatusCode();
            if (status == 200) {
                HttpEntity entityResponse = httpResponse.getEntity();
                long contentLength = entityResponse.getContentLength();
//                if (contentLength <= 0)
//                    throw new IOException("No response");
                if (contentLength > 0) {
                    return IOUtils.toByteArray(entityResponse.getContent());
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            httpPost.releaseConnection();
            httpPost.completed();
        }
        return null;
    }
    public static String post(String url, HttpClient client, Map<String, String> param, String cookies) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(2000)
                .setConnectTimeout(2000).build());

        List<NameValuePair> paramslist = new ArrayList<NameValuePair>();

        if (param != null) {
            for (Map.Entry<String, String> entry : param.entrySet()) {
                paramslist.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        if (cookies != null) {
            httpPost.addHeader(new BasicHeader("Cookie", cookies));
        }

        UrlEncodedFormEntity uefEntity = null;
        try {
            uefEntity = new UrlEncodedFormEntity(paramslist, utf8);
        } catch (UnsupportedEncodingException e) {
        }

        httpPost.setEntity(uefEntity);
        try {
            HttpResponse response = client.execute(httpPost);
            // 获取请求状态
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), utf8));
                StringBuilder sb = new StringBuilder();
                String line = null;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            httpPost.releaseConnection();
            httpPost.completed();
        }
        return null;
    }

}
