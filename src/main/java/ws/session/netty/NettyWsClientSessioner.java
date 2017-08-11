package ws.session.netty;

import ws.WsConstants;
import ws.client.WsProxyClient;
import ws.protocol.WsTsPortHandle;
import ws.session.WsClientSession;
import ws.session.WsClientSessionSenderBase;
import ws.session.WsSessionAPI;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public class NettyWsClientSessioner extends WsClientSessionSenderBase {

    private NettyWsClientSession nettyWsClientSession;


    public NettyWsClientSessioner(WsProxyClient _wsProxyClient) {
        super(_wsProxyClient);
        nettyWsClientSession = (NettyWsClientSession) _wsProxyClient.getWsClientSession();
        serializeManager = _wsProxyClient.getWsContainer().getSerializeManager();
    }

    @Override
    public void onOpen(WsClientSession session) {
        WsConstants.wslogger.warn("Client Session[id=" + session.id() + "] is connected");
    }

    @Override
    public void onClose() {
        WsConstants.wslogger.warn("Client Session [id=" + nettyWsClientSession.id() + "] is closed!!!!");
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
        return nettyWsClientSession;
    }

    @Override
    protected WsTsPortHandle wsTsPortHandle() {
        return wsProxyClient.getWsContainer().getClientWsTsPortHandle();
    }

    @Override
    protected void fireOnCloseEvent() {
        this.onClose();
    }


}
