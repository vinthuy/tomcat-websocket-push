package ws.client;


import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import ws.Constants;
import ws.SenderApi;
import ws.model.WsResult;

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
public class WsProxyClient implements SenderApi {

    private WebSocketContainer container;
    //格式:"ws://localhost:8080/ws/pushClient.ws"
    private String url;

    public WsProxyClient(String url) {
        this.url = url;
        container = ContainerProvider.getWebSocketContainer();
        newSession();
    }

    private static final byte[] heartBytes = new byte[]{1,0,1};

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

    public boolean heart() {
        try {
            session.getBasicRemote().sendPing(ByteBuffer.wrap(heartBytes));
        } catch (IOException e) {
            Constants.wslogger.error("wsClient send ping error ", e);
            try {
                session.close();
            }catch (Exception ex){
                //忽略
            }
            return false;
        }
        return true;
    }

    public Session getSession() {
        return session;
    }

    @Override
    public void SendWsResultProtocol(WsResult wsResult) {
        String msg = null;
        try {
            msg = JSONObject.toJSONString(wsResult);
        } catch (Exception e) {
            Constants.wslogger.error("wsClient parse message error: " + e.getMessage(), e);
        }

        if (StringUtils.isNotBlank(msg)) {
            sendText(msg);
        }

    }
}
