package ws.server;

/**
 * the push server session lisenter.
 * Created by ruiyong.hry on 14/07/2017.
 */
public interface ServerSessionListener {

    public void onStart(PushToClientSession session);

    public void onClose(PushToClientSession session);

    public void onError(PushToClientSession session);
}
