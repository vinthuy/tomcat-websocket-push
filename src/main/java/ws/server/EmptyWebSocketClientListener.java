package ws.server;


import ws.Constants;

/**
 * Created by ruiyong.hry on 02/07/2017.
 */
public class EmptyWebSocketClientListener implements WebSocketClientListener {


    public void handlePreSendingMsg(Object msg) {

    }

    public void onSelectSessionError(Object msg) {
        Constants.wslogger.error("Notice:Not find useful web-socket-client session.");
    }
}
