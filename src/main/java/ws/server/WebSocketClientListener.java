package ws.server;

/**
 * the websocket client status listener
 * Created by ruiyong.hry on 02/07/2017.
 */
public interface WebSocketClientListener {

    //������Ҫ���͵���Ϣ
    public void handlePreSendingMsg(Object msg);

    /**
     * ��·��ѡ�����Ĵ���ʽ
     *
     * @param msg
     */
    public void onSelectSessionError(Object msg);
}
