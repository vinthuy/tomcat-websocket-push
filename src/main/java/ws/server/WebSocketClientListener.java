package ws.server;

/**
 * the websocket client status listener
 * Created by ruiyong.hry on 02/07/2017.
 */
public interface WebSocketClientListener {

    //处理即将要发送的消息
    public void handlePreSendingMsg(Object msg);

    /**
     * 在路由选择出错的处理方式
     *
     * @param msg
     */
    public void onSelectSessionError(Object msg);
}
