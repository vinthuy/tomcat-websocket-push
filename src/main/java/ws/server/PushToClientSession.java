package ws.server;


import ws.*;
import ws.protocol.WsTsPortHandle;
import ws.session.WsSessionAPI;
import ws.session.j2ee.J2EEWsClientSession;
import ws.session.j2ee.J2EEWsSession;
import org.apache.commons.lang3.StringUtils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 1.web-socket push server
 * 2.generate a instance when a web-socket was created  successfully.
 * 3.连接的协议中必须是p=?并且在我们平台有所注册,且必须放第一个
 */
@ServerEndpoint(value = "/ws/pushClient.ws", configurator = SessionConfigurator.class)
public class PushToClientSession extends ServerSessionSenderBase {

    private String key;
    private String clientHost;
    private J2EEWsSession j2EEWsSession;
    private ServerConnValidDO serverConnValidDO;

    public PushToClientSession() {
        super(WsContainerSingle.instance().getPushServer());
    }


    @OnOpen
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        Map<String, List<String>> parameters = session.getRequestParameterMap();
        clientHost = getParam(parameters, WsConstants.clientHost);
        if (StringUtils.isBlank(clientHost)) {
            return;
        }
        WsConstants.wslogger.warn("server Session [id=" + session.getId() + "] is connect successfully,clientHost=" + clientHost);

        j2EEWsSession = new J2EEWsClientSession(session);
        session.setMaxTextMessageBufferSize(8192 * 10);
        session.setMaxIdleTimeout(120 * 1000);

        String key = getParam(parameters, "p");
        if (StringUtils.isNotBlank(key)) {
            ServerConnValidDO serverConnValidDO = wsSessionGroupManager.validAndHandleGroupKey(key, clientHost);
            if (serverConnValidDO.isValid()) {
                this.serverConnValidDO = serverConnValidDO;
                this.key = serverConnValidDO.getGroupKey();
                pushServer.addPushToClientSession(serverConnValidDO.getGroupKey(), this);
                for (ServerSessionListener serverSessionListener : pushServer.getServerSessionListeners()) {
                    serverSessionListener.onStart(this);
                }
                return;
            }

        }
        CloseReason.CloseCode closeCode = new CloseReason.CloseCode() {

            @Override
            public int getCode() {
                return -500;
            }
        };
        try {
            session.close(new CloseReason(closeCode, "非法连接!!p=" + key));
        } catch (IOException error) {
            WsConstants.wslogger.error("server Session[id=" + session.getId() + "] has error:" + error.getMessage(), error);
        }


    }

    @OnClose
    public void onClose() {
        if (j2EEWsSession != null) {
            WsConstants.wslogger.warn("server Session [id=" + j2EEWsSession.id() + "] is closed!clientHost=" + clientHost);
        }
        if (StringUtils.isNotBlank(key)) {
            pushServer.removePushToClientSession(key, this);
            for (ServerSessionListener serverSessionListener : pushServer.getServerSessionListeners()) {
                serverSessionListener.onClose(this);
            }
        }
        pushDataRequestMap = null;
    }

    @OnMessage
    public void processFragment(byte[] responseData, boolean isLast, Session session) {
        super.processFra(responseData, isLast, true);
    }


    /**
     * 接受到客户端消息的处理
     * <p>
     * //     * @param message
     *
     * @param session
     */
    @OnError
    public void onError(Session session, Throwable error) {
        WsConstants.wslogger.error("server Session[id=" + session.getId() + "] has error:" + error.getMessage(), error);
        for (ServerSessionListener serverSessionListener : pushServer.getServerSessionListeners()) {
            serverSessionListener.onError(this);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PushToClientSession)) return false;
        PushToClientSession that = (PushToClientSession) o;
        return Objects.equals(j2EEWsSession, that.j2EEWsSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(j2EEWsSession);
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public J2EEWsSession getSession() {
        return j2EEWsSession;
    }

    @Override
    public String toString() {
        if (j2EEWsSession != null)
            return com.google.common.base.Objects.toStringHelper(this)
                    .add("key", key).add("sessionId", j2EEWsSession.id())
                    .toString();
        return "sessionNull";
    }


    public PushServer getPushServer() {
        return pushServer;
    }

    public String getClientHost() {
        return clientHost;
    }

    public void setClientHost(String clientHost) {
        this.clientHost = clientHost;
    }

    @Override
    protected WsSessionAPI wsSessionAPI() {
        return j2EEWsSession;
    }

    @Override
    protected WsTsPortHandle wsTsPortHandle() {
        return pushServer.getWsContainer().getClientWsTsPortHandle();
    }

    @Override
    protected void fireOnCloseEvent() {
        this.onClose();
    }

    public boolean isOpen() {
        return j2EEWsSession.isOpen();
    }

    public String getSessionId() {
        return j2EEWsSession.id();
    }

    public ServerConnValidDO getServerConnValidDO() {
        return serverConnValidDO;
    }
}