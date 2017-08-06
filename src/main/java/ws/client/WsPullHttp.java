package ws.client;

import ws.Constants;
import ws.model.PullMsg;
import ws.util.HttpTools;
import ws.util.WsUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruiyong.hry on 25/07/2017.
 */
public abstract class WsPullHttp implements WsPull {

    private String requestUrlSuffix = "/api/pushPullApi.do";


    @Override
    public String pull(PullMsg pullMsg) {
        Map<String, String> param = new HashMap<String, String>();
        param.put("key", pullMsg.getKey());
        param.put("msgKey", pullMsg.getMsgKey());
        Constants.wslogger.warn(String.format("request %s:%s", pullMsg.getKey(), pullMsg.getMsgKey()));
        String realUrl = getPullServerUrl(pullMsg.getKey()) + requestUrlSuffix;
        String remoteResult = HttpTools.post(realUrl, WsUtil.getHttpClient(), param, null);
        Constants.wslogger.warn(pullMsg.getMsgKey()+":response->" + remoteResult);
        if (realUrl != null && !remoteResult.startsWith("-")) {
            return remoteResult;
        }
        return null;
    }

    public abstract String getPullServerUrl(String key);
}
