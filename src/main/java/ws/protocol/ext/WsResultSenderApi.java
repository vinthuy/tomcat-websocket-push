package ws.protocol.ext;

import ws.model.WsResult;
import ws.protocol.SenderApi;
import ws.server.PushServer;

/**
 * Created by ruiyong.hry on 04/08/2017.
 */
public class WsResultSenderApi implements SenderApi<WsResult> {

    private PushServer pushServer;

    public WsResultSenderApi(PushServer pushServer) {
        this.pushServer = pushServer;
    }

    @Override
    public WsResult SendWsResultProtocol(String clienKey, WsResult wsResult, boolean sync) {
        return pushServer.sendObj(clienKey, wsResult, sync);
    }
}
