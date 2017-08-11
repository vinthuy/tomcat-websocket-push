package ws.session;

import ws.SessionSenderBase;
import ws.client.WsProxyClient;
import ws.serialize.SerializeManager;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public abstract class WsClientSessionSenderBase extends SessionSenderBase implements WsClientSessionListener{

    protected WsProxyClient wsProxyClient;

    protected SerializeManager serializeManager;


    public WsClientSessionSenderBase(WsProxyClient wsProxyClient) {
        this.wsProxyClient = wsProxyClient;
        serializeManager = wsProxyClient.getWsContainer().getSerializeManager();
    }




    protected SerializeManager serializeManager() {
        return this.serializeManager;
    }
}
