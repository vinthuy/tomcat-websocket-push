package ws.server;


import ws.Constants;
import ws.WsContainer;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Objects;

/**
 * 1.web-socket push server
 * 2.generate a instance when a web-socket was created  successfully.
 */
@ServerEndpoint("/pushClient.ws")
public class PushToClientSession {

    PushServer pushServer = WsContainer.instance().getPushServer();

    private Session session;

    public PushToClientSession() {
        if (pushServer == null) {
            throw new RuntimeException("The Push-Server is not started!!plz start");
        }
    }


    @OnOpen
    public void onOpen(Session session) {
        Constants.wslogger.info("server Session [id=" + session.getId() + "] is connect successfully");
        this.session = session;
        pushServer.addPushToClientSession(this);
    }

    @OnClose
    public void onClose() {
        Constants.wslogger.warn("server Session [id=" + session.getId() + "] is closed!!!!");
        pushServer.removePushToClientSession(this);
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
        Constants.wslogger.error("server Session[id=" + session.getId() + "] has error:" + error.getMessage(),error);
    }

    public void sendMessage(String message) throws IOException {
        if(this.session.isOpen()){
            this.session.getBasicRemote().sendText(message);
        }else {
            try {
                this.session.close();
            }catch (Exception e){
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