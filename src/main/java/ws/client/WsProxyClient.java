package ws.client;


import ws.Constants;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * the web-socket client.
 * 保持客户端一个连接用于推送
 * Created by ruiyong.hry on 02/07/2017.
 */
public class WsProxyClient {

    private WebSocketContainer container;
    //格式:"ws://localhost:8080/ws/"
    private String url;

    private URI uri;

    public WsProxyClient(String url) {
        this.url = url;
        container = ContainerProvider.getWebSocketContainer();
        uri = URI.create(url);
        newSession();
    }

    private volatile boolean abledConnect = true;

    private volatile long connectCount = 0;


    private Session session;

    private ByteBuffer byteBuffer;

    public boolean isDisabledConnect() {
        return !abledConnect;
    }

    public void newSession() {
        //当超过了500连接错误,不再连接了
        if (connectCount > 500 & abledConnect) {
            abledConnect = false;
        }

        if (isDisabledConnect()) {
            return;
        }

        try {
            Constants.wslogger.warn("Connecting to " + url);
            session = container.connectToServer(WsProxyClientEndpoint.class, uri);
            byteBuffer = ByteBuffer.allocate(Constants.requestHeartBytes.length);
        } catch (Exception ex) {
            connectCount++;
            Constants.wslogger.error("wsClient newSession err: " + ex.getMessage());
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

    public boolean heart() {
        try {
            //这儿不能用ping/pong操作,只能实现逻辑的心跳检测
            byteBuffer.clear();
            byteBuffer.put(Constants.requestHeartBytes);
            byteBuffer.flip();
            session.getBasicRemote().sendBinary(byteBuffer);
//            session.getBasicRemote().sendBinary(ByteBuffer.wrap(Constants.reponseHeartBytes));
        } catch (Exception e) {
            Constants.wslogger.error("wsClient send ping error " + e.getMessage());
            try {
                session.close();
            } catch (Exception ex) {
                //忽略
            }
            return false;
        }
        return true;
    }

    public Session getSession() {
        return session;
    }

//    @Override
//    public void SendWsResultProtocol(WsResult wsResult) {
//        String msg = null;
//        try {
//            msg = JSONObject.toJSONString(wsResult);
//        } catch (Exception e) {
//            Constants.wslogger.error("wsClient parse message error: " + e.getMessage(), e);
//        }
//
//        if (StringUtils.isNotBlank(msg)) {
//            sendText(msg);
//        }
//
//    }

    public String getUrl() {
        return url;
    }
}
