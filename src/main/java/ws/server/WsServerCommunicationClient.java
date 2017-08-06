package ws.server;

import ws.server.ext.WsServerInterResult;

/**
 * 服务器内部通信接口
 * Created by ruiyong.hry on 14/07/2017.
 */
public interface WsServerCommunicationClient{
    WsServerInterResult send(String key, Object message, String serverIp, int port, boolean sync) throws Exception;
}
