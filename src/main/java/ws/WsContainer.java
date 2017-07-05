package ws;


import ws.client.WsProxyClient;
import ws.server.PushServer;

import javax.websocket.Session;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * the web-socket container
 * Created by ruiyong.hry on 02/07/2017.
 */
public class WsContainer {

    private static WsContainer wsContainer = new WsContainer();

    private PushServer pushServer;

    private List<WsProxyClient> wsProxyClients;

    private Map<PushMsgType, WsResultHandler> resultHandlerMap = new ConcurrentHashMap<PushMsgType, WsResultHandler>();

    private int wsProxyClientMaxCount = 2;

    //ҵ�����̳߳�,��Ϊ�ɲ�ͬtomcat���̳߳ع���,�Ѵﵽ�ɿ���.
    private ExecutorService clientThreadPool = Executors.newFixedThreadPool(2 * wsProxyClientMaxCount);


    //����߳�
    private ScheduledExecutorService checkTh;


    WsContainer() {
        wsProxyClients = new ArrayList<WsProxyClient>();
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

    public static void initServer() {
        wsContainer.pushServer = new PushServer();
    }

    public interface WsConfigFace<T extends WsContainer.WsConfigDO> {
        boolean initWsClient(T wsConfigDO);
    }

    public static class WsConfigDO implements Serializable {

    }

    /**
     * trigger ws client  in start wsclient.
     *
     * @param wsConfigFace
     * @param wsConfigDO
     */
    public void triggerClient(final WsConfigFace wsConfigFace, final WsConfigDO wsConfigDO) {
        if(wsConfigFace.initWsClient(wsConfigDO)){
            checkTh = Executors.newSingleThreadScheduledExecutor();
            checkTh.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    List<WsProxyClient> list = WsContainer.instance().getWsProxyClients();
                    if (list==null||list.isEmpty()) {
                        wsConfigFace.initWsClient(wsConfigDO);
                    } else {
                        for (WsProxyClient wsProxyClient : list) {
                            //�Զ�����
                            if (wsProxyClient.getSession() == null || !wsProxyClient.getSession().isOpen()) {
                                wsProxyClient.newSession();
                            }else {
                                if(!wsProxyClient.heart()){
                                    wsProxyClient.newSession();
                                }
                            }
                        }
                    }
                }
            }, 120, 90, TimeUnit.SECONDS);
        }
    }


    public boolean newWsProxyClient(String url) {
        if (wsProxyClients.size() >= wsProxyClientMaxCount) {
            return false;
        }
        synchronized (this) {
            if (wsProxyClients.size() >= wsProxyClientMaxCount) {
                return false;
            }
            WsProxyClient wsProxyClient = new WsProxyClient(url);
            wsProxyClients.add(wsProxyClient);
        }
        return true;
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


    public WsProxyClient getWsProxyClient(Session session) {
        for (WsProxyClient wsProxyClient : wsProxyClients) {
            if (wsProxyClient.getSession().getId().equals(session.getId())) {
                return wsProxyClient;
            }
        }
        return null;
    }

    public List<WsProxyClient> getWsProxyClients() {
        return wsProxyClients;
    }

    public ExecutorService getClientThreadPool() {
        return clientThreadPool;
    }


}
