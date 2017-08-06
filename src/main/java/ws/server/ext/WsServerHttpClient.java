package ws.server.ext;

import HttpTools;
import ws.Constants;
import ws.serialize.SerializeManager;
import ws.server.WsServerCommunicationClient;
import ws.util.WsUtil;
import org.apache.commons.lang.ArrayUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruiyong.hry on 14/07/2017.
 */
public class WsServerHttpClient implements WsServerCommunicationClient {

    private String requestUrlSuffix = "/api/pushApi.do";

    private SerializeManager serializeManager;

    public WsServerHttpClient(SerializeManager serializeManager) {
        this.serializeManager = serializeManager;
    }

    //    private HttpClient httpClient;


//    public WsServerHttpClient() {
//        httpClient = createtHttpClient();
//    }
//
//    private HttpClient createtHttpClient() {
//        return HttpClients.createDefault();
//    }

    @Override
    public WsServerInterResult send(String key, Object message, String serverIp, int port, boolean sync) throws Exception {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("key", key);
        param.put("message", message);
        param.put("sync", Boolean.toString(sync));
        Constants.wslogger.warn(String.format("request %s:%s:%s", serverIp, port, param));
//        String result = HttpTools.post(getServerIpUrl(serverIp, port), WsUtil.getHttpClient(), param, null);
        byte[] requestBytes = serializeManager.serialize(param);
        if (ArrayUtils.isEmpty(requestBytes)) {
            Constants.wslogger.error("WsServerHttpClient request is null");
            return null;
        }
        WsServerInterResult wsServerInterResult = null;
        byte[] result = HttpTools.postByte(getServerIpUrl(serverIp, port), WsUtil.getHttpClient(), requestBytes);
        if (!ArrayUtils.isEmpty(result)) {
            wsServerInterResult = (WsServerInterResult) serializeManager.deserialize(result);
        }
        Constants.wslogger.warn(String.format("response %s:%s:%s", serverIp, port, wsServerInterResult));
        return wsServerInterResult;
    }

    private String getServerIpUrl(String serverIp, int port) {
        return new StringBuilder("http://").append(serverIp).append(":").append(port).append(requestUrlSuffix).toString();
    }
}
