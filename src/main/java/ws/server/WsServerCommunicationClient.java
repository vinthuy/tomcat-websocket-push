package ws.server;

import ws.server.ext.WsServerInterResult;

/**
 * �������ڲ�ͨ�Žӿ�
 * Created by ruiyong.hry on 14/07/2017.
 */
public interface WsServerCommunicationClient{
    WsServerInterResult send(String key, Object message, String serverIp, int port, boolean sync) throws Exception;
}
