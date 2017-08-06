package ws.util;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import ws.Constants;
import ws.model.PullMsg;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by ruiyong.hry on 25/07/2017.
 */
public class WsUtil {

    private static final String pullMsgPrefix = "pull";

    private static final String pullMsgPrefixSplit = "@";

    public static String genPullMsgHeader(String msgKey) {
        return new StringBuilder(pullMsgPrefix).append(pullMsgPrefixSplit).append(genCurEnvKey()).append(pullMsgPrefixSplit).append(msgKey).toString();
    }


    public static String genCurEnvKey() {
        return "";
    }


    public static boolean isPullMsgHeader(String msg) {
        if (msg != null & msg.startsWith(pullMsgPrefix + pullMsgPrefixSplit)) {
            return true;
        }
        return false;
    }

    public static PullMsg splitMsgKey(String msg) {
        if (StringUtils.isNotBlank(msg)) {
            String[] arry = msg.split(pullMsgPrefixSplit);
            if (arry.length >= 3) {
                PullMsg pullMsg = new PullMsg();
                pullMsg.setKey(arry[1]);
                pullMsg.setMsgKey(arry[2]);
                return pullMsg;
            }
        }
        return null;
    }

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
            Constants.wslogger.error("joinByte error :" + e.getMessage(), e);
        }
        return byteArrayOutputStream.toByteArray();
    }


    private static HttpClient httpClient = HttpClients.createDefault();

    public static HttpClient getHttpClient() {
        return httpClient;
    }

}
