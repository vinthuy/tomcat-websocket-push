package ws.session.j2ee;

import ws.WsConstants;
import ws.client.WsProxyClient;
import ws.protocol.WsTsPortHandle;
import ws.serialize.SerializeManager;
import ws.session.WsClientSession;
import ws.session.WsClientSessionListener;
import ws.session.WsClientSessionSenderBase;
import ws.session.WsSessionAPI;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public class J2EEWsClientSessioner extends WsClientSessionSenderBase implements WsClientSessionListener {

    private J2EEWsClientSession j2EEWsClientSession;

    public J2EEWsClientSessioner(WsProxyClient _wsProxyClient) {
        super(_wsProxyClient);
        j2EEWsClientSession = (J2EEWsClientSession) _wsProxyClient.getWsClientSession();
    }


    @Override
    public void onOpen(WsClientSession session) {
        WsConstants.wslogger.warn("Client Session[id=" + session.id() + "] is connected");
    }

    @Override
    public void onClose() {
        WsConstants.wslogger.warn("Client Session [id=" + j2EEWsClientSession.id() + "] is closed!!!!");
        //自动重连
        if (wsProxyClient != null) {
            WsConstants.wslogger.warn("Session reconnecting");
            wsProxyClient.newSession();
        }
    }

    @Override
    public void onError(WsClientSession session, Throwable t) {
        WsConstants.wslogger.error("WsProxyClientEndpoint error: " + t.getMessage(), t);
    }

    @Override
    public void processFragment(byte[] responseData, boolean isLast) {
        super.processFra(responseData, isLast, false);
    }


    @Override
    protected WsSessionAPI wsSessionAPI() {
        return j2EEWsClientSession;
    }

    @Override
    protected WsTsPortHandle wsTsPortHandle() {
        return null;
    }

    @Override
    protected void fireOnCloseEvent() {
        this.onClose();
    }


    public void setJ2EEWsClientSession(J2EEWsClientSession j2EEWsClientSession) {
        this.j2EEWsClientSession = j2EEWsClientSession;
    }

    public void setWsProxyClient(WsProxyClient wsProxyClient) {
        this.wsProxyClient = wsProxyClient;
    }

    public void setSerializeManager(SerializeManager serializeManager) {
        this.serializeManager = serializeManager;
    }
}
