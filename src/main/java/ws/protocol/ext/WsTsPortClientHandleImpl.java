package ws.protocol.ext;

import com.alibaba.fastjson.JSON;
import ws.*;
import ws.client.WsPull;
import ws.model.PullMsg;
import ws.model.WsResult;
import ws.protocol.WsTsPortHandle;
import ws.util.WsUtil;
import org.apache.commons.lang.StringUtils;

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
            if (WsUtil.isPullMsgHeader(message)) {
                PullMsg pullMsg = WsUtil.splitMsgKey(message);
                if (pullMsg == null) {
                    Constants.wslogger.error("pull msg is err.");
                }
                WsPull wsPull = WsContainerSingle.instance().getWsPull();
                if (wsPull == null) {
                    Constants.wslogger.error("Not support pull big data");
                }
                message = wsPull.pull(pullMsg);
                if (StringUtils.isBlank(message)) {
                    Constants.wslogger.error("pull msg is null or error");
                    return null;
                }
            }
            WsResult wsResult = JSON.parseObject(message, WsResult.class);
            if (wsResult.isSuccess()) {
                WsResultHandler wsResultHandler = findWsResultHandler(wsResult.getFlag());
                if (wsResultHandler != null) {
                    return wsResultHandler.handle(wsResult);
                } else {
                    Constants.wslogger.warn("message is dropped:" + message);
                    wsResultHandler = new OkWsResultHandlerImpl();
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
