package ws.session;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public interface WsClientSession extends WsSessionAPI {

    public boolean heart();

    public boolean isSameId(WsClientSession other);
}
