package ws.util;

import ws.WsConstants;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by ruiyong.hry on 25/07/2017.
 */
public class WsUtil {


    public static byte[] joinByte(ConcurrentLinkedDeque<byte[]> pushRequestData) {
        if (pushRequestData == null) {
            return new byte[0];
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            for (byte[] b : pushRequestData) {
                byteArrayOutputStream.write(b);
            }
            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();
        } catch (IOException e) {
            WsConstants.wslogger.error("joinByte error :" + e.getMessage(), e);
        }
        return byteArrayOutputStream.toByteArray();
    }


    private static HttpClient httpClient = HttpClients.createDefault();

    public static HttpClient getHttpClient() {
        return httpClient;
    }


}
