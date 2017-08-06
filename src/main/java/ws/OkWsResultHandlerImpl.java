package ws;


import com.alibaba.fastjson.JSONObject;
import ws.model.WsResult;

/**
 * Created by ruiyong.hry on 02/07/2017.
 */
public class OkWsResultHandlerImpl extends WsResultHandler {
    public WsResult handle(WsResult wsRequest) {
        System.out.println(JSONObject.toJSONString(wsRequest));
        wsRequest.setData("finished");
        return wsRequest;
    }

}
