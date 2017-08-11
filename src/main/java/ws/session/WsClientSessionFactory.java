package ws.session;

import ws.client.WsProxyClient;

/**
 * the WsClient Session Factory
 * Created by ruiyong.hry on 10/08/2017.
 */
public interface WsClientSessionFactory<T extends WsClientSession> {

    public T newSession(WsProxyClient wsProxyClient) throws Exception;

    public void destory();

    <E extends WsClientSessionListener> E newWsClientSessionListener(WsProxyClient wsProxyClient);

}
