package ws;

import ws.client.WsPull;
import ws.protocol.WsTsPortHandle;
import ws.protocol.ext.WsTsPortClientHandleImpl;
import ws.serialize.HessionSerializeManager;
import ws.serialize.SerializeManager;

/**
 * Created by ruiyong.hry on 04/08/2017.
 */
public class WsContainerSingle  {

    private static WsContainer wsContainer;


    public static WsContainer instance() {
        if(wsContainer==null){
            synchronized (WsContainerSingle.class){
                if(wsContainer == null){
                    wsContainer = new WsContainer();
                    wsContainer.setSerializeManager(new HessionSerializeManager());
                    wsContainer.setClientWsTsPortHandle(new WsTsPortClientHandleImpl());
                }
            }
        }
        return wsContainer;
    }


    public static WsContainer build(SerializeManager serializeManager, WsTsPortHandle wsTsPortHandle, WsPull wsPull) {
        if(wsContainer==null){
            synchronized (WsContainerSingle.class){
                if(wsContainer == null){
                    wsContainer = new WsContainer();
                    wsContainer.setSerializeManager(serializeManager);
                    wsContainer.setClientWsTsPortHandle(wsTsPortHandle);
                    wsContainer.setWsPull(wsPull);
                }
            }
        }
        return wsContainer;
    }


}
