package ws;


import com.google.common.collect.Maps;
import ws.buffer.ByteBufferAllocator;
import ws.client.WsProxyClient;
import ws.protocol.WsTsPortHandle;
import ws.serialize.SerializeManager;
import ws.server.PushServer;
import ws.session.WsClientSessionFactory;
import ws.util.AssertUtils;
import ws.util.HostServerUtil;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * the web-socket container
 * 注意,所有的对外api都需要调用check()方法
 * Created by ruiyong.hry on 02/07/2017.
 */
public class WsContainer {


    private PushServer pushServer;

    private Map<String, WsProxyClient> wsProxyClientMap;


    @Getter
    private WsConfigDO wsConfigDO;


    //检测线程
    private ScheduledExecutorService checkTh;

    private volatile boolean configStarted = false;

    //序列化manager
    private SerializeManager serializeManager;

    //客户端协议处理器--根据具体消息处理
    private WsTsPortHandle clientWsTsPortHandle;

    //连接相关session安全验证等处理
    private WsSessionGroupManager wsSessionGroupManager;


    WsContainer() {
        wsProxyClientMap = Maps.newConcurrentMap();
        checkTh = Executors.newSingleThreadScheduledExecutor();
    }


    //初始化pushServer
    private void initServer(WsConfigFace wsConfigFace) {
        HostServerUtil.setPort(wsConfigDO.getServerPort());
        pushServer = PushServer.preInstance(this);
        wsConfigFace.buildPushServer(pushServer);
        pushServer.create();
        checkTh.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    pushServer.synchronizedPushToClientSessionToCaChe();
                } catch (Exception e) {
                    WsConstants.wslogger.error("synchronizedPushToClientSessionToCaChe err:" + e.getMessage(), e);
                }
            }
        }, 60, wsConfigDO.serverSyncSessionInterval, TimeUnit.SECONDS);
    }

    /**
     * 对于扩展容器都需要实现此方法来启动push-server,wsClient
     *
     * @param <T>
     */
    public interface WsConfigFace<T extends WsContainer.WsConfigDO> {

        boolean initWsClient(T wsConfigDO);

        void checkWsClient(T wsConfigDO, Collection<WsProxyClient> list);

        /**
         * //pushtoSession listener
         * private ServerSessionListener serverSessionListener;
         * private SessionManager sessionManager;
         * private WsServerCommunicationClient wsServerCommunicationClient;
         *
         * @param pushServer
         */
        void buildPushServer(PushServer pushServer);

    }

    //默认实现,单手启动api
    public static class WsConfigFaceDefault implements WsConfigFace {
        @Override
        public boolean initWsClient(WsConfigDO wsConfigDO) {
            return true;
        }

        @Override
        public void buildPushServer(PushServer pushServer) {
        }

        @Override
        public void checkWsClient(WsConfigDO wsConfigDO, Collection list) {

        }
    }

    @Data
    public static class WsConfigDO implements Serializable {
        //默认10一次心跳
        protected int heartInterval = 15;
        protected int wsProxyClientMaxCount = 10;
        protected int retryCountWhenSessionServerIpFail = 10;
        protected int serverPort = 80;
        //服务器同步session间隔 默认60s
        protected int serverSyncSessionInterval = 180;
        //发送字符串超过 就走推拉结合方式传输 5K
        protected int pullStrOrSplitAtLength = 7168;
        //推送超时
        protected int pushDataTimeout = 1000 * 30;

        //是否启动push-server
        protected boolean wsServerStart = true;
        //是否启动wsClient连接push-server
        protected boolean wsClientStart = true;
        protected int env;
        //两分钟超时,仅对netty框架有效
        protected int sessionTimeOut = 120;
    }


    /**
     * 只有是wsClient才能使用
     *
     * @param wsConfigFace
     * @param wsConfigDO
     */
    public synchronized void triggerStartOnWsClientMode(final WsConfigFace wsConfigFace, final WsConfigDO wsConfigDO) {
        if (wsConfigDO.isWsServerStart()) {
            throw new WsException("please use triggerStart method if wsServerStart is true");
        }
        triggerStart(wsConfigFace, wsConfigDO, null);
    }

    /**
     * 根据配置触发客户端初始化(启动入口)
     *
     * @param wsConfigFace
     * @param wsConfigDO
     * @param wsSessionGroupManager 对连接session进行管理
     */
    public synchronized void triggerStart(final WsConfigFace wsConfigFace, final WsConfigDO wsConfigDO, WsSessionGroupManager wsSessionGroupManager) {
        if (configStarted) {
            return;
        }
        this.wsConfigDO = wsConfigDO;
        HostServerUtil.setCurrentServerEnv(wsConfigDO.getEnv());

        this.wsSessionGroupManager = wsSessionGroupManager;
//        heartBufferPool = ByteBufferPool.newInstance(6).newAllocator(1000);
        configStarted = true;
        if (wsConfigDO.isWsServerStart()) {
            initServer(wsConfigFace);
        }
        if (wsConfigDO.isWsClientStart() && wsConfigFace.initWsClient(wsConfigDO)) {
            checkTh.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        Collection<WsProxyClient> list = wsProxyClientMap.values();
                        wsConfigFace.checkWsClient(wsConfigDO, list);

                        if (CollectionUtils.isNotEmpty(list)) {
                            for (WsProxyClient wsProxyClient : list) {
                                //自动重连
                                if (wsProxyClient.getWsClientSession() == null || !wsProxyClient.getWsClientSession().isOpen()) {
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
                        WsConstants.wslogger.error("triggerStart.err:" + e.getMessage(), e);
                    }

                }
            }, 45, wsConfigDO.getHeartInterval(), TimeUnit.SECONDS);
        }

    }


    public boolean newWsProxyClient(String url, WsClientSessionFactory wsClientSessionFactory) {
        check();
        if (wsProxyClientMap.size() > wsConfigDO.wsProxyClientMaxCount) {
            WsConstants.wslogger.warn(String.format(" %s not connect,wsProxyClientMaxCount:%s", url, wsConfigDO.getWsProxyClientMaxCount()));
            return false;
        }
        synchronized (this) {
            if (wsProxyClientMap.size() > wsConfigDO.wsProxyClientMaxCount) {
                return false;
            }
            WsProxyClient wsProxyClient = new WsProxyClient(this, url, wsClientSessionFactory);
            if(wsProxyClient.getSessionId()!=null){
                wsProxyClientMap.put(wsProxyClient.getSessionId(), wsProxyClient);
            }
        }
        return true;
    }


    public PushServer getPushServer() {
        check();
        return pushServer;
    }


    public WsProxyClient getWsProxyClient(String sessionId) {
        check();
        return wsProxyClientMap.get(sessionId);
    }

    /**
     * 根据请求地址获取wsProxyClient
     *
     * @param url
     * @return
     */
    public WsProxyClient getWsProxyClientByUrlAddr(String url) {
        check();
        for (Map.Entry<String, WsProxyClient> entry : wsProxyClientMap.entrySet()) {
            WsProxyClient wsProxyClient = entry.getValue();
            if (StringUtils.equals(wsProxyClient.getUrl(), url)) {
                return wsProxyClient;
            }
        }
        return null;
    }

    /**
     * 返回第一个连接
     *
     * @return
     */
    public WsProxyClient getWsProxyClientRadmon() {
        check();
        for (Map.Entry<String, WsProxyClient> entry : wsProxyClientMap.entrySet()) {
            WsProxyClient wsProxyClient = entry.getValue();
            if (wsProxyClient != null) {
                return wsProxyClient;
            }
        }
        return null;
    }


    private void check() {
        if (!configStarted) {
            throw new RuntimeException("wsContainer is not configStarted!!!");
        }
        if (wsConfigDO.isWsServerStart()) {
            AssertUtils.notNull(wsSessionGroupManager);
        }
        AssertUtils.notNull(clientWsTsPortHandle);
        AssertUtils.notNull(serializeManager);
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

    public WsSessionGroupManager getWsSessionGroupManager() {
        return wsSessionGroupManager;
    }

    public void setWsSessionGroupManager(WsSessionGroupManager wsSessionGroupManager) {
        this.wsSessionGroupManager = wsSessionGroupManager;
    }

    public static ByteBufferAllocator heartBufferPool;


    //推送ID生成器
    private static AtomicLong PushGenID = new AtomicLong(0);

    public static long genPushDataId() {
        return PushGenID.incrementAndGet();
    }


}
