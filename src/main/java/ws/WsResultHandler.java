package ws;


import ws.model.WsResult;

/**
 * ��Ϣ������
 * Created by ruiyong.hry on 02/07/2017.
 */
public interface WsResultHandler<W> {

    public WsResult handle(WsResult wsRequest);

    public W getObj(WsResult wsResult);


}
