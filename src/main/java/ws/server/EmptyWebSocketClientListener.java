package ws.server;


import ws.WsConstants;

/**
 * Created by ruiyong.hry on 02/07/2017.
 */
public class EmptyWebSocketClientListener implements WebSocketClientListener {


    public void handlePreSendingMsg(Object msg) {

    }

    public void onSelectSessionError(Object msg) {
        WsConstants.wslogger.error("Notice:Not find useful web-socket-client session.");
    }
}
