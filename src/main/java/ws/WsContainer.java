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
 * ע��,���еĶ���api����Ҫ����check()����
 * Created by ruiyong.hry on 02/07/2017.
 */
public class WsContainer {


    private PushServer pushServer;

    private Map<String, WsProxyClient> wsProxyClientMap;


    @Getter
    private WsConfigDO wsConfigDO;


    //����߳�
    private ScheduledExecutorService checkTh;

    private volatile boolean configStarted = false;

    //���л�manager
    private SerializeManager serializeManager;

    //�ͻ���Э�鴦����--���ݾ�����Ϣ����
    private WsTsPortHandle clientWsTsPortHandle;

    //�������session��ȫ��֤�ȴ���
    private WsSessionGroupManager wsSessionGroupManager;


    WsContainer() {
        wsProxyClientMap = Maps.newConcurrentMap();
        checkTh = Executors.newSingleThreadScheduledExecutor();
    }


    //��ʼ��pushServer
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
     * ������չ��������Ҫʵ�ִ˷���������push-server,wsClient
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

    //Ĭ��ʵ��,��������api
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
        //Ĭ��10һ������
        protected int heartInterval = 15;
        protected int wsProxyClientMaxCount = 10;
        protected int retryCountWhenSessionServerIpFail = 10;
        protected int serverPort = 80;
        //������ͬ��session��� Ĭ��60s
        protected int serverSyncSessionInterval = 180;
        //�����ַ������� ����������Ϸ�ʽ���� 5K
        protected int pullStrOrSplitAtLength = 7168;
        //���ͳ�ʱ
        protected int pushDataTimeout = 1000 * 30;

        //�Ƿ�����push-server
        protected boolean wsServerStart = true;
        //�Ƿ�����wsClient����push-server
        protected boolean wsClientStart = true;
        protected int env;
        //�����ӳ�ʱ,����netty�����Ч
        protected int sessionTimeOut = 120;
    }


    /**
     * ֻ����wsClient����ʹ��
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
     * �������ô����ͻ��˳�ʼ��(�������)
     *
     * @param wsConfigFace
     * @param wsConfigDO
     * @param wsSessionGroupManager ������session���й���
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
                                //�Զ�����
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
     * ���������ַ��ȡwsProxyClient
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
     * ���ص�һ������
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


    //����ID������
    private static AtomicLong PushGenID = new AtomicLong(0);

    public static long genPushDataId() {
        return PushGenID.incrementAndGet();
    }


}
