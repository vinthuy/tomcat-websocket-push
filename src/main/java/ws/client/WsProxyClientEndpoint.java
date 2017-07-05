package ws.client;

import com.alibaba.fastjson.JSON;
import ws.Constants;
import ws.WsContainer;
import ws.WsResultHandler;
import ws.model.WsResult;

import javax.websocket.*;

/**
 * websocket代理客户端
 * 接受来自服务端的推送消息
 *
 * @author ruiyong.hry
 */
@ClientEndpoint
public class WsProxyClientEndpoint {

    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        Constants.wslogger.info("Connected to endpoint: " + session.getBasicRemote());
        this.session = session;
    }

    @OnClose
    public void onClose() {
        Constants.wslogger.warn("Session [id=" + session.getId() + "] is closed!!!!");
    }

    /**
     * 这个消息是服务端推送的.
     * 对于客户端我们采集多线程处理
     *
     * @param message
     */
    @OnMessage
    public void processMessage(String message, Session session) {
        //1.转换成可识别的json
        WsContainer.instance().getClientThreadPool().submit(new WsProxyClientTask(message, session));
    }

    @OnError
    public void processError(Throwable t) {
        Constants.wslogger.error("WsProxyClientEndpoint error: " + t.getMessage(), t);
    }

    static class WsProxyClientTask implements Runnable {

        private String message;
        private Session session;

        public WsProxyClientTask(String message, Session session) {
            this.message = message;
            this.session = session;
        }

        public void run() {
            try {
                WsResult wsResult = JSON.parseObject(message, WsResult.class);
                if (wsResult.isSuccess()) {
                    WsResultHandler wsResultHandler = WsContainer.instance().findWsResultHandler(wsResult.getFlag());
                    if (wsResultHandler != null) {
                        WsResult wsrsp = wsResultHandler.handle(wsResult);
                        if (wsrsp != null) {
                            WsContainer.instance().getWsProxyClient(session).SendWsResultProtocol(wsrsp);
                        }
                    } else {
                        Constants.wslogger.warn("message is dropped:" + message);
                    }
                }
            } catch (Exception e) {
                //消息不合符贵方丢弃
                Constants.wslogger.error("Handle message failed!!->" + e.getMessage(), e);
            }

        }
    }

}