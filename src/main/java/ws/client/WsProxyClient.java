package ws.client;


import ws.Constants;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;

/**
 * the web-socket client.
 * 保持客户端一个连接用于推送
 * Created by ruiyong.hry on 02/07/2017.
 */
public class WsProxyClient {

    private WebSocketContainer container;
    //格式:"ws://localhost:8080/ws/"
    private String url;

    public WsProxyClient(String url) {
        this.url = url;
        container = ContainerProvider.getWebSocketContainer();
    }

    private Session session;

    public void newSession() {
        try {
            Constants.wslogger.info("Connecting to " + url);
            session = container.connectToServer(WsProxyClientEndpoint.class, URI.create(url));
        } catch (Exception ex) {
            Constants.wslogger.error(ex.getMessage(), ex);
            if (session != null) {
                try {
                    session.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendText(String message) {
        if (session == null) {
            synchronized (this) {
                if (session == null) {
                    newSession();
                }
            }
        }
        if (!session.isOpen()) {
            //新建一个session
            synchronized (this) {
                newSession();
            }
        }

        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            Constants.wslogger.error("wsClient send message error: " + message, e);
        }
    }

}
