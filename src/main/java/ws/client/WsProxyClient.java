package ws.client;


import ws.WsConstants;
import ws.WsContainer;
import ws.session.*;

import java.net.URI;

/**
 * the web-socket client.
 * 保持客户端一个连接用于推送
 * Created by ruiyong.hry on 02/07/2017.
 */
public class WsProxyClient {


    private String url;

    private URI uri;

    private WsClientSessionFactory sessionFactory;

    private WsClientSessionSenderBase sessesionHander;

    private String sessionId;

    private WsContainer wsContainer;

    public WsProxyClient(WsContainer wsContainer, String url, WsClientSessionFactory sessionFactory) {
        this.wsContainer = wsContainer;
        this.url = url;
        uri = URI.create(url);
        this.sessionFactory = sessionFactory;
        newSession();
    }

    private volatile boolean abledConnect = true;

    private volatile long connectCount = 0;


    private WsClientSession wsClientSession;


    public void newSession() {
        //当超过了500连接错误,不再连接了
        if (connectCount > 500 & abledConnect) {
            abledConnect = false;
        }

        if (isDisabledConnect()) {
            return;
        }

        try {
            WsConstants.wslogger.warn("Connecting to " + url);
            wsClientSession = sessionFactory.newSession(this);
            sessionId = wsClientSession.id();
            sessesionHander = (WsClientSessionSenderBase) sessionFactory.newWsClientSessionListener(this);
        } catch (Exception ex) {
            connectCount++;
            WsConstants.wslogger.error("wsClient newSession err: " + ex.getMessage());
            if (wsClientSession != null) {
                try {
                    wsClientSession.close();
                } catch (Exception e) {
                    WsConstants.wslogger.error("wsClient newSession err:" + e.getMessage());
                }
            }
        }
    }


    public WsProxyClient getSessionIfCloseNew() {
        if (wsClientSession != null && wsClientSession.isOpen()) {
            return this;
        }
        newSession();
        return this;
    }

    public boolean isDisabledConnect() {
        return !abledConnect;
    }

    public boolean heart() {
        return wsClientSession.heart();
    }

    public <RS> RS sendObj(Object message, boolean sync) throws Exception {
        return sessesionHander.sendMessage(message, sync);
    }


    public long getConnectCount() {
        return connectCount;
    }

    public void setConnectCount(long connectCount) {
        this.connectCount = connectCount;
    }

    public boolean isAbledConnect() {
        return abledConnect;
    }

    public void setAbledConnect(boolean abledConnect) {
        this.abledConnect = abledConnect;
    }

    public String getUrl() {
        return url;
    }

    public WsClientSession getWsClientSession() {
        return wsClientSession;
    }


    public String getSessionId() {
        return sessionId;
    }

    public WsContainer getWsContainer() {
        return wsContainer;
    }

    public URI getUri() {
        return uri;
    }

    public WsClientSessionSenderBase getSessionHandler() {
        return sessesionHander;
    }
}
