package ws.server;

import org.apache.commons.lang3.StringUtils;
import ws.Constants;
import ws.WsContainer;
import wshandle.WsConfig;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Objects;

/**
 * 1.web-socket push server
 * 2.generate a instance when a web-socket was created  successfully.
 * 3.连接的协议中必须是p=?并且在我们平台有所注册,且必须放第一个
 */
@ServerEndpoint("/ws/pushClient.ws")
public class PushToClientSession {

    PushServer pushServer = WsContainer.instance().getPushServer();

    private Session session;

    private String key;

    public PushToClientSession() {
        if (pushServer == null) {
            throw new RuntimeException("The Push-Server is not started!!plz start");
        }
    }


    @OnOpen
    public void onOpen(Session session) {
        Constants.wslogger.info("server Session [id=" + session.getId() + "] is connect successfully");
        this.session = session;
        String queryString = session.getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            String[] queryArray = queryString.split("=");
            if (queryArray.length > 1) {
                String p = queryArray[0];
                if (p.equalsIgnoreCase("p")) {
                    String key = queryString.split("=")[1];
                    if (StringUtils.isNotBlank(key)) {
                        if (WsConfig.PushServerKey.get(key) != null) {
                            this.key = key;
                            pushServer.addPushToClientSession(key, this);
                            return;
                        }
                    }
                }
            }

        }
        CloseReason.CloseCode closeCode = new CloseReason.CloseCode() {

            @Override
            public int getCode() {
                return -100;
            }
        };
        try {
            session.close(new CloseReason(closeCode, "Not support q = "));
        } catch (IOException error) {
            Constants.wslogger.error("server Session[id=" + session.getId() + "] has error:" + error.getMessage(), error);
        }


    }

    @OnClose
    public void onClose() {
        Constants.wslogger.warn("server Session [id=" + session.getId() + "] is closed!!!!");
        pushServer.removePushToClientSession(key, this);
    }

    /**
     * 接受到客户端消息的处理
     *
     * @param message
     * @param session
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        Constants.wslogger.info("server receive the message:" + message);
    }


    @OnError
    public void onError(Session session, Throwable error) {
        Constants.wslogger.error("server Session[id=" + session.getId() + "] has error:" + error.getMessage(), error);
    }

    public void sendMessage(String message) throws IOException {
        if (this.session.isOpen()) {
            this.session.getBasicRemote().sendText(message);
        } else {
            try {
                this.session.close();
            } catch (Exception e) {
                //
            }
        }
        //this.session.getAsyncRemote().sendText(message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PushToClientSession)) return false;
        PushToClientSession that = (PushToClientSession) o;
        return Objects.equals(session, that.session);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session);
    }


}