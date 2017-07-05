package ws;


import ws.model.WsResult;

/**
 * Created by ruiyong.hry on 04/07/2017.
 */
public interface SenderApi {

    public void sendText(String message);

    void SendWsResultProtocol(WsResult wsResult);
}
