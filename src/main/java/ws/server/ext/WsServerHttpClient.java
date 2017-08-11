package ws.server.ext;

import ws.WsConstants;
import ws.serialize.SerializeManager;
import ws.server.WsServerCommunicationClient;
import ws.util.HttpTools;
import ws.util.WsUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruiyong.hry on 14/07/2017.
 */
public abstract class WsServerHttpClient implements WsServerCommunicationClient {


    private SerializeManager serializeManager;

    public WsServerHttpClient(SerializeManager serializeManager) {
        this.serializeManager = serializeManager;
    }


    @Override
    public WsServerInterResult send(String key, Object message, String serverIp, int port, boolean sync) throws Exception {
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("key", key);
        param.put("message", message);
        param.put("sync", Boolean.toString(sync));
        WsConstants.wslogger.warn(String.format("request %s:%s:%s", serverIp, port, param));
//        String result = HttpTools.post(getServerIpUrl(serverIp, port), WsUtil.getHttpClient(), param, null);
        byte[] requestBytes = serializeManager.serialize(param);
        if (ArrayUtils.isEmpty(requestBytes)) {
            WsConstants.wslogger.error("WsServerHttpClient request is null");
            return null;
        }
        WsServerInterResult wsServerInterResult = null;
        byte[] result = HttpTools.postByte(getServerIpUrl(serverIp, port), WsUtil.getHttpClient(), requestBytes);
        if (!ArrayUtils.isEmpty(result)) {
            wsServerInterResult = (WsServerInterResult) serializeManager.deserialize(result);
        }
        WsConstants.wslogger.warn(String.format("response %s:%s:%s", serverIp, port, wsServerInterResult));
        return wsServerInterResult;
    }

    protected abstract String getServerIpUrl(String serverIp, int port);

}
