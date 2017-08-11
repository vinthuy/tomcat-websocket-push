package ws;

import ws.serialize.SerializeManager;
import ws.server.PushServer;

import java.io.IOException;

/**
 * Created by ruiyong.hry on 04/08/2017.
 */
public abstract class ServerSessionSenderBase extends SessionSenderBase {

    protected PushServer pushServer;
    protected SerializeManager serializeManager;
    protected WsSessionGroupManager wsSessionGroupManager;


    public ServerSessionSenderBase(PushServer pushServer) {
        this.pushServer = pushServer;
        this.serializeManager = pushServer.getWsContainer().getSerializeManager();
        this.wsSessionGroupManager = pushServer.getWsContainer().getWsSessionGroupManager();
    }


    protected SerializeManager serializeManager() {
        return serializeManager;
    }


    public void heartResponse() {
        try {
            wsSessionAPI().sendBinary(WsConstants.reponseHeartBytes);
        } catch (Exception e) {
            if (e instanceof IOException) {
                try {
                    if (wsSessionAPI() != null) {
                        wsSessionAPI().close();
                    }
                } catch (IOException e1) {

                }
            }
        }
    }


}
