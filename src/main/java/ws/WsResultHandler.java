package ws;


import ws.model.WsResult;

/**
 * ��Ϣ������
 * Created by ruiyong.hry on 02/07/2017.
 */
public abstract class WsResultHandler<W> {

    public abstract WsResult handle(WsResult wsRequest);

    public W getObj(WsResult wsResult) {
        return (W) wsResult.getData();
    }

}
