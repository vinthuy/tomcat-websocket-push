package ws.session;

/**
 * the ws client session listen
 * Created by ruiyong.hry on 10/08/2017.
 */
public interface WsClientSessionListener {

    public void onOpen(WsClientSession session);

    public void onClose();

    public void onError(WsClientSession session, Throwable t);

    public void processFragment(byte[] responseData, boolean isLast);
}
