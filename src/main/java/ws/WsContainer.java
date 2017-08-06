package ws;


import ws.client.WsProxyClient;
import ws.client.WsPull;
import ws.protocol.WsTsPortHandle;
import ws.protocol.ext.WsResultSenderApi;
import ws.serialize.SerializeManager;
import ws.server.PushServer;
import ws.server.ext.WsServerHttpClient;
import ws.util.HostServerUtil;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;

import javax.websocket.Session;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * the web-socket container
 * 注意,所有的对外api都需要调用check()方法
 * Created by ruiyong.hry on 02/07/2017.
 */
public class WsContainer {


    private PushServer pushServer;

    private List<WsProxyClient> wsProxyClients;


    @Getter
    private WsConfigDO wsConfigDO;


    //检测线程
    private ScheduledExecutorService checkTh;

    private volatile boolean configStarted = false;

    private WsPull wsPull;

    private SerializeManager serializeManager;

    private WsTsPortHandle clientWsTsPortHandle;


    WsContainer() {
        wsProxyClients = new ArrayList<WsProxyClient>();
        checkTh = Executors.newSingleThreadScheduledExecutor();
    }


    //初始化pushServer
    private void initServer() {
        pushServer = new PushServer();
        setServerParam();
        checkTh.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    pushServer.synchronizedPushToClientSessionToCaChe();
                } catch (Exception e) {
                    Constants.wslogger.error("synchronizedPushToClientSessionToCaChe err:" + e.getMessage(), e);
                }
            }
        }, 60, wsConfigDO.serverSyncSessionInterval, TimeUnit.SECONDS);
    }

    //设置服务器参数
    private void setServerParam() {
        pushServer.setWsServerCommunicationClient(new WsServerHttpClient(serializeManager));
        pushServer.setRetryCountWhenSessionServerIpFail(wsConfigDO.getRetryCountWhenSessionServerIpFail());
        HostServerUtil.setPort(wsConfigDO.getServerPort());
        pushServer.setSenderApi(new WsResultSenderApi(pushServer));
    }

    public interface WsConfigFace<T extends WsContainer.WsConfigDO> {
        boolean initWsClient(T wsConfigDO);

        boolean postWsServer(T wsConfigDO);

        void checkWsClient(T wsConfigDO, List<WsProxyClient> list);
    }

    //默认实现,单手启动api
    public static class WsConfigFaceDefault implements WsConfigFace {
        @Override
        public boolean initWsClient(WsConfigDO wsConfigDO) {
            return true;
        }

        @Override
        public boolean postWsServer(WsConfigDO wsConfigDO) {
            return true;
        }

        @Override
        public void checkWsClient(WsConfigDO wsConfigDO, List list) {

        }
    }

    @Data
    public static class WsConfigDO implements Serializable {
        //默认10一次心跳
        private int heartInterval = 12;
        private int wsProxyClientMaxCount = 4;
        private int retryCountWhenSessionServerIpFail = 10;
        private int serverPort = 80;
        //服务器同步session间隔 默认60s
        private int serverSyncSessionInterval = 90;
        //发送字符串超过 就走推拉结合方式传输 5K
        private int pullStrLength = 5120;
        //推送超时
        private int pushDataTimeout = 1000 * 30;
    }


    /**
     * 根据配置触发客户端初始化(启动入口)
     *
     * @param wsConfigFace
     * @param wsConfigDO
     */
    public synchronized void triggerStart(final WsConfigFace wsConfigFace, final WsConfigDO wsConfigDO) {
        if (configStarted) {
            return;
        }
        this.wsConfigDO = wsConfigDO;
        configStarted = true;
        initServer();
        wsConfigFace.postWsServer(wsConfigDO);

        if (wsConfigFace.initWsClient(wsConfigDO)) {

            checkTh.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<WsProxyClient> list = getWsProxyClients();
                        wsConfigFace.checkWsClient(wsConfigDO, list);

                        if (CollectionUtils.isNotEmpty(list)) {
                            for (WsProxyClient wsProxyClient : list) {
                                //自动重连
                                if (wsProxyClient.getSession() == null || !wsProxyClient.getSession().isOpen()) {
                                    wsProxyClient.newSession();
                                } else {
                                    if (wsProxyClient.isDisabledConnect()) {
                                        return;
                                    }

                                    if (!wsProxyClient.heart()) {
                                        wsProxyClient.newSession();
                                    }
                                }
                            }
                        }
                    } catch (Throwable e) {
                        Constants.wslogger.error("triggerStart.err:" + e.getMessage(), e);
                    }

                }
            }, 45, wsConfigDO.getHeartInterval(), TimeUnit.SECONDS);
        }

    }


    public boolean newWsProxyClient(String url) {
        check();
        if (wsProxyClients.size() > wsConfigDO.wsProxyClientMaxCount) {
            return false;
        }
        synchronized (this) {
            if (wsProxyClients.size() > wsConfigDO.wsProxyClientMaxCount) {
                return false;
            }
            WsProxyClient wsProxyClient = new WsProxyClient(url);
            wsProxyClients.add(wsProxyClient);
        }
        return true;
    }


    public PushServer getPushServer() {
        check();
        return pushServer;
    }


    public WsProxyClient getWsProxyClient(Session session) {
        check();
        for (WsProxyClient wsProxyClient : wsProxyClients) {
            if (wsProxyClient.getSession() != null && wsProxyClient.getSession().getId().equals(session.getId())) {
                return wsProxyClient;
            }
        }
        return null;
    }

    private void check() {
        if (!configStarted) {
            throw new RuntimeException("wsContainer is not configStarted!!!");
        }
    }

    public List<WsProxyClient> getWsProxyClients() {
        return wsProxyClients;
    }


    public void setWsPull(WsPull wsPull) {
        this.wsPull = wsPull;
    }

    public WsPull getWsPull() {
        return wsPull;
    }

    public void setSerializeManager(SerializeManager serializeManager) {
        this.serializeManager = serializeManager;
    }

    public SerializeManager getSerializeManager() {
        return serializeManager;
    }

    public WsTsPortHandle getClientWsTsPortHandle() {
        return clientWsTsPortHandle;
    }

    public void setClientWsTsPortHandle(WsTsPortHandle clientWsTsPortHandle) {
        this.clientWsTsPortHandle = clientWsTsPortHandle;
    }

    private DistributeCacheService distributeCacheService;

    public DistributeCacheService getDistributeCacheService() {
        return distributeCacheService;
    }

    public void setDistributeCacheService(DistributeCacheService distributeCacheService) {
        this.distributeCacheService = distributeCacheService;
    }
}
