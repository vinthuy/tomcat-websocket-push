package ws;


import ws.model.WsResult;

/**
 * 消息处理器
 * Created by ruiyong.hry on 02/07/2017.
 */
public interface WsResultHandler {

    public WsResult handle(WsResult wsRequest);

}
