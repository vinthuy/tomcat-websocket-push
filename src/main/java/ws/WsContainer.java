package ws;


import ws.client.WsProxyClient;
import ws.server.PushServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * the web-socket container
 * Created by ruiyong.hry on 02/07/2017.
 */
public class WsContainer {

    private static WsContainer wsContainer = new WsContainer();

    private PushServer pushServer;

    private WsProxyClient wsProxyClient;

    private ExecutorService clientThreadPool = Executors.newFixedThreadPool(2);

    private Map<PushMsgType, WsResultHandler> resultHandlerMap = new ConcurrentHashMap<PushMsgType, WsResultHandler>();


    WsContainer() {
        registerWsResultHandler(PushMsgType.OK, new OkWsResultHandlerImpl());

    }

    public void registerWsResultHandler(PushMsgType pushMsgTyp, WsResultHandler wsResultHandler) {
        resultHandlerMap.put(pushMsgTyp, wsResultHandler);
    }

    public WsResultHandler findWsResultHandler(int f) {
        PushMsgType pushMsgType = PushMsgType.getPushMsgType(f);
        if (pushMsgType != null) {
            return resultHandlerMap.get(pushMsgType);
        }
        return null;
    }

    public static class PushMsgType {

        private static List<PushMsgType> list = new ArrayList<PushMsgType>();

        public PushMsgType(int tag) {
            this.tag = tag;
            list.add(this);
        }

        private int tag;

        public int getTag() {
            return tag;
        }


        public static PushMsgType OK = new PushMsgType(0);

        public static PushMsgType getPushMsgType(int f) {
            for (PushMsgType pushMsgType : list) {
                if (pushMsgType.getTag() == f) {
                    return pushMsgType;
                }
            }
            return null;
        }
    }


    //³õÊ¼»¯²Ù×÷
    public static void initServer() {
        wsContainer.pushServer = new PushServer();
    }

    public static void initClient(String url) {
        wsContainer.wsProxyClient = new WsProxyClient(url);
    }

    public static WsContainer instance() {
        return wsContainer;
    }


    public PushServer getPushServer() {
        if (pushServer == null) {
            synchronized (this) {
                if (pushServer == null) {
                    initServer();
                }
            }
        }
        return pushServer;
    }


    public WsProxyClient getWsProxyClient() {
        return wsProxyClient;
    }


    public ExecutorService getClientThreadPool() {
        return clientThreadPool;
    }


}
