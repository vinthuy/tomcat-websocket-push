package ws.protocol.ext;

import com.alibaba.fastjson.JSON;
import ws.PushMsgType;
import ws.WsConstants;
import ws.WsResultHandler;
import ws.model.WsResult;
import ws.protocol.WsTsPortHandle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruiyong.hry on 04/08/2017.
 */
public class WsTsPortClientHandleImpl implements WsTsPortHandle<Object, WsResult> {


    private Map<PushMsgType, WsResultHandler> resultHandlerMap = new ConcurrentHashMap<PushMsgType, WsResultHandler>();

    public void registerWsResultHandler(PushMsgType pushMsgTyp, WsResultHandler wsResultHandler) {
        resultHandlerMap.put(pushMsgTyp, wsResultHandler);
    }

    public WsResultHandler findWsResultHandler(int f) {
        PushMsgType pushMsgType = PushMsgType.getPushMsgType(f);
        if (pushMsgType != null) {
            return resultHandlerMap.get(pushMsgType);
        }
        return null;
    }

    @Override
    public WsResult handle(Object request) {
        //1.首先判断是否是需要拉取消息,兼容老的方案
        if (request instanceof String) {
            String message = (String) request;
            WsResult wsResult = JSON.parseObject(message, WsResult.class);
            if (wsResult.isSuccess()) {
                WsResultHandler wsResultHandler = findWsResultHandler(wsResult.getFlag());
                if (wsResultHandler != null) {
                    return wsResultHandler.handle(wsResult);
                } else {
                    WsConstants.wslogger.warn("message is dropped:" + message);
                    return wsResultHandler.handle(wsResult);
                }
            }
        } else if (request instanceof WsResult) {
            WsResult wsResult = (WsResult) request;
            if (wsResult.isSuccess()) {
                WsResultHandler wsResultHandler = findWsResultHandler(wsResult.getFlag());
                if (wsResultHandler != null) {
                    return wsResultHandler.handle(wsResult);
                }
            }
        }
        return null;
    }
}
