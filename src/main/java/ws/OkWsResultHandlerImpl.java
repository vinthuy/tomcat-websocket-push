package ws;


import com.alibaba.fastjson.JSONObject;
import ws.model.WsResult;

/**
 * Created by ruiyong.hry on 02/07/2017.
 */
public class OkWsResultHandlerImpl implements WsResultHandler {
    public WsResult handle(WsResult wsRequest) {
        System.out.println(JSONObject.toJSONString(wsRequest));
        return WsResult.buildSimpleSuccessResult();
    }

    @Override
    public Object getObj(WsResult wsResult) {
        return wsResult.getData();
    }
}
