package ws.session.j2ee;

import ws.client.WsProxyClient;
import ws.client.WsProxyClientEndpoint;
import ws.session.WsClientSessionFactory;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * the J2ee wsClient session factory.
 * Created by ruiyong.hry on 10/08/2017.
 */
public class J2EEWsClientSessionFactory implements WsClientSessionFactory<J2EEWsClientSession> {

    @Override
    public J2EEWsClientSession newSession(WsProxyClient wsProxyClient) throws Exception {
        //当超过了500连接错误,不再连接了
        //格式:"ws://localhost:8080/ws/"
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        Session session = container.connectToServer(WsProxyClientEndpoint.class, wsProxyClient.getUri());
        J2EEWsClientSession j2EEWsClientSession = new J2EEWsClientSession(session);
        return j2EEWsClientSession;
    }

    @Override
    public void destory() {

    }

    @Override
    public J2EEWsClientSessioner newWsClientSessionListener(WsProxyClient wsProxyClient) {
        return new J2EEWsClientSessioner(wsProxyClient);
    }
}
