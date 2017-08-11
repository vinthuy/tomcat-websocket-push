package ws.server;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import ws.WsConstants;
import ws.WsContainer;
import ws.WsException;
import ws.model.EnvCacheValue;
import ws.model.ServerEndpoint;
import ws.protocol.SenderApi;
import ws.protocol.ext.WsResultSenderApi;
import ws.server.ext.ServerSelectorImpl;
import ws.server.ext.WsServerInterResult;
import ws.util.AssertUtils;
import ws.util.HostServerUtil;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * push server.
 * Created by ruiyong.hry on 02/07/2017.
 */
public class PushServer {


    //����proxyClientListeners�Ĵ�С
    private ConcurrentHashMap<String, ConcurrentSkipListMap<String, PushToClientSession>> proxyClientListenersMap;

    //��Ĭ�ϵ��ֶ�
    private Selectetor selector;
    private WebSocketClientListener webSocketClientListener;
    private ServerSelector serverSelector;
    //Ѱ��session ���Դ��� (����ֲ�ʽ������������,һ���Ϊ������ȥ1)
    private int retryCountWhenSessionServerIpFail = 10;
    private SenderApi senderApi;

    //����Ҫע����ֶ�---------------start-----
    //pushtoSession listener
    private Set<ServerSessionListener> serverSessionListeners;
    private SessionManager sessionManager;
    private WsServerCommunicationClient wsServerCommunicationClient;
    //��Ҫע����ֶ�---------------end-----

    //ѡ��ע���ֶ�
    private MsgForwarder msgForwarder;

    private volatile boolean isNewed = false;

    private WsContainer wsContainer;

    private PushServer(WsContainer wsContainer) {
        this.wsContainer = wsContainer;
        serverSessionListeners = Sets.newConcurrentHashSet();
    }

    public static PushServer preInstance(WsContainer wsContainer) {
        return new PushServer(wsContainer);
    }

    public PushServer create() {
        checkCondition();
        proxyClientListenersMap = new ConcurrentHashMap<String, ConcurrentSkipListMap<String, PushToClientSession>>();
        if (selector == null) {
            selector = new SelectetorImpl();
        }
        if (webSocketClientListener == null) {
            webSocketClientListener = new EmptyWebSocketClientListener();
        }
        if (serverSelector == null) {
            serverSelector = new ServerSelectorImpl();
        }
        if (senderApi == null) {
            senderApi = new WsResultSenderApi(this);
        }
        isNewed = true;
        return this;
    }

    private void checkCondition() {
        AssertUtils.notNull(sessionManager, "sessionManager is null");
        AssertUtils.notNull(wsServerCommunicationClient, "wsServerCommunicationClient is null");
    }

    private void checkInstanced() {
        AssertUtils.isTrue(isNewed, "The push server is not created!!");
    }


    public boolean addPushToClientSession(String key, PushToClientSession pushToClientListener) {
        ConcurrentSkipListMap<String, PushToClientSession> list = proxyClientListenersMap.get(key);
        if (list == null) {
            list = new ConcurrentSkipListMap<String, PushToClientSession>();
            proxyClientListenersMap.put(key, list);
        }
        try {
            if (pushToClientListener.getSession() != null) {
                list.put(pushToClientListener.getSession().id(), pushToClientListener);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean removePushToClientSession(String key, PushToClientSession pushToClientListener) {
        if (pushToClientListener.getSession() == null)
            return false;
        ConcurrentSkipListMap<String, PushToClientSession> list = proxyClientListenersMap.get(key);
        if (list != null && list.size() > 0) {
            try {
                if (pushToClientListener.getSession() != null) {
                    list.remove(pushToClientListener.getSession().id());
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }


    public <RS> RS sendObj(String key, Object message, boolean sync) {
        checkInstanced();
        //test--code
//        WsServerInterResult wsServerInterResult = null;
//        try {
//            wsServerInterResult = wsServerCommunicationClient.send(key, message, HostServerUtil.getLocalIp(), HostServerUtil.getPort(), sync);
//        } catch (Exception e) {
//            if (e instanceof BizException) {
//                throw (BizException) e;
//            }
//            WsConstants.wslogger.error("wsServerCommunicationClient.request.error->" + e.getMessage(), e);
//        }
//
//        if (wsServerInterResult != null) {
//            return (RS) wsServerInterResult.getData();
//        }
//        return null;
//      return   executeSendInOtherServerIp(key, message, sync);
        //-----��������
        ConcurrentSkipListMap<String, PushToClientSession> list = proxyClientListenersMap.get(key);
        if (MapUtils.isEmpty(list)) {
            //�Ҳ����͵���api �ҵ����õ�serverIP,���е���
            return executeSendInOtherServerIp(key, message, sync);
        }
        return executeSendLocal(key, message, sync);
    }


//    /**
//     * ����ִ������
//     */
//    public <RS> RS executeSendLocal(String key, String txt) {
//        return executeSendLocal(key, txt, false);
//    }

    public <RS> RS executeSendLocal(String key, Object msg, boolean sync) {
        checkInstanced();
        ConcurrentSkipListMap<String, PushToClientSession> list = proxyClientListenersMap.get(key);
        if (MapUtils.isEmpty(list)) {
            WsConstants.wslogger.warn("Not find useful push-to-session");
            throw new WsException("���Ϳͻ���û�з���,����30s�������!");
        }

        try {
            PushToClientSession pushToClientListener = selector.select(list);
            if (pushToClientListener != null && pushToClientListener.getSession() != null) {
                if (pushToClientListener != null) {
                    webSocketClientListener.handlePreSendingMsg(msg);
                    if (msg != null) {
                        return pushToClientListener.sendMessage(msg, sync);
                    }
                } else {
                    webSocketClientListener.onSelectSessionError(msg);
                }
            } else {
                WsConstants.wslogger.warn("Not find wsClient,message = [" + msg + "] dropped");
            }
        } catch (WsException e) {
            throw e;
        } catch (Exception e) {
            WsConstants.wslogger.error("send mesasage error:" + e.getMessage(), e);
            throw new WsException("���Ϳͻ��˷�������,����30s�������!");
        }
        return null;
    }

    /**
     * ������������ִ������
     *
     * @param key
     * @return
     */
    private <RS> RS executeSendInOtherServerIp(String key, Object message, boolean sync) {
        List<EnvCacheValue> envCacheValues = getSessionManager().getAllSessionCount(key);
        if (envCacheValues == null) {
            WsConstants.wslogger.error(String.format(" %s :%s not find all session", HostServerUtil.getLocalIp(), HostServerUtil.getPort()));
            throw new WsException("û�����Ϳͻ�������,����30s�������!");
        }

        WsServerInterResult<RS> wsServerInterResult = null;
        for (int i = 0; i < retryCountWhenSessionServerIpFail; i++) {
            ServerEndpoint serverEnpoint = serverSelector.router(key, envCacheValues);

            WsConstants.wslogger.warn(String.format("executeSendInOtherServerIp find %s ", serverEnpoint));

            if (serverEnpoint == null) {
                throw new WsException("û�����Ϳͻ�������,����30s�������!");
            }

            try {
                wsServerInterResult = wsServerCommunicationClient.send(key, message, serverEnpoint.getServerIp(), serverEnpoint.getPort(), sync);
            } catch (Exception e) {
                if (e instanceof WsException) {
                    throw (WsException) e;
                }
                WsConstants.wslogger.error("wsServerCommunicationClient.request.error->" + e.getMessage(), e);
            }

            if (wsServerInterResult == null) {
                throw new WsException("����Զ�����ͽ����������,����30s�������!,serverEndpoint=" + serverEnpoint);
            }

            //�������ɹ�,��ô������,���������
            if (wsServerInterResult.isSuccess()) {
                return wsServerInterResult.getData();
            }
            //�ߵ����,˵��û�гɹ�,ɾ������
            for (int j = envCacheValues.size() - 1; j >= 0; j--) {
                EnvCacheValue envCacheValue = envCacheValues.get(j);
                if (envCacheValue == null || ObjectUtils.equals(envCacheValue.getServerEndpoint(), serverEnpoint)) {
                    envCacheValues.remove(j);
                }
            }
        }
        if (wsServerInterResult != null && !wsServerInterResult.isSuccess()) {
            throw new WsException("·�ɴ���" + wsServerInterResult.getMsg());
        }
        return null;
    }


    /**
     * ͬ��push-to-client��cache
     */
    public void synchronizedPushToClientSessionToCaChe() {
        if (MapUtils.isNotEmpty(proxyClientListenersMap)) {
            for (Map.Entry<String, ConcurrentSkipListMap<String, PushToClientSession>> proxyClientListenersEntry : proxyClientListenersMap.entrySet()) {
                //ֱ��ͬ������
                String key = proxyClientListenersEntry.getKey();
                int cacheSessionCount = 0;
                int realSessionCount = 0;
                EnvCacheValue envCacheValue = sessionManager.getCurrentServerIpSessionCount(key);
                if (envCacheValue != null) {
                    cacheSessionCount = envCacheValue.getSessionCount();
                }
                ConcurrentSkipListMap<String, PushToClientSession> values = proxyClientListenersEntry.getValue();
                if (values != null) {
                    realSessionCount = values.size();
                }

                WsConstants.wslogger.warn(String.format("synchronizedPushToClientSessionToCaChe key:%s cacheSessionCount:%s->realSessionCount:%s", new Object[]{key, cacheSessionCount, realSessionCount}));
                //����ͬ��ͬ��
                if (cacheSessionCount != realSessionCount) {
                    sessionManager.setSessionCount(key, realSessionCount);
                }

            }
        }
    }

    public <RS> void SendWsResultProtocol(String clientKey, RS rs) {
        senderApi.SendWsResultProtocol(clientKey, rs, false);
    }

    public <RS> RS SendWsResultProtocol(String clientKey, RS rs, boolean sync) {
        return (RS) senderApi.SendWsResultProtocol(clientKey, rs, sync);
    }

    //wsResultЭ�鷢����
    public <RS> RS SendWsResultProtocol(GroupKeySelector groupKeySelector, RS message, boolean sync) {
        String key = groupKeySelector.selectByMessage(message);
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return (RS) senderApi.SendWsResultProtocol(key, message, sync);
    }

    public interface MsgParse {
        public String parse(Object msg);
    }


    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public PushServer buildSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        return this;
    }

    public Set<ServerSessionListener> getServerSessionListeners() {
        return serverSessionListeners;
    }

    public PushServer addServerSessionListener(ServerSessionListener serverSessionListener) {
        serverSessionListeners.add(serverSessionListener);
        return this;
    }

    public PushServer buildRetryCountWhenSessionServerIpFail(int retryCountWhenSessionServerIpFail) {
        this.retryCountWhenSessionServerIpFail = retryCountWhenSessionServerIpFail;
        return this;
    }

    public MsgForwarder getMsgForwarder() {
        return msgForwarder;
    }

    public PushServer buildMsgForwarder(MsgForwarder msgForwarder) {
        this.msgForwarder = msgForwarder;
        return this;
    }

    public PushServer buildSelector(Selectetor selectetor) {
        this.selector = selectetor;
        return this;
    }

    public Map<String, List<String>> dumpSession() {
        Map<String, List<String>> map = Maps.newHashMap();
        for (Map.Entry<String, ConcurrentSkipListMap<String, PushToClientSession>> proxyClientListenersEntry : proxyClientListenersMap.entrySet()) {
            String key = proxyClientListenersEntry.getKey();
            ConcurrentSkipListMap<String, PushToClientSession> pushToClientSessionMap = proxyClientListenersEntry.getValue();
            List<String> list = Lists.newArrayList();
            if (pushToClientSessionMap != null) {
                for (PushToClientSession pushToClientSession : pushToClientSessionMap.values()) {
                    String clientIP = pushToClientSession.getClientHost();
                    if (pushToClientSession.getSession() != null) {
                        list.add(StringUtils.join(new Object[]{pushToClientSession.getSession().id(), "@", clientIP}));
                    }
                }
            }
            map.put(key, list);
        }
        return map;
    }


    public SenderApi getSenderApi() {
        return senderApi;
    }

    public PushServer buildSenderApi(SenderApi senderApi) {
        this.senderApi = senderApi;
        return this;
    }

    public WsServerCommunicationClient getWsServerCommunicationClient() {
        return wsServerCommunicationClient;
    }

    public void buildWsServerCommunicationClient(WsServerCommunicationClient wsServerCommunicationClient) {
        this.wsServerCommunicationClient = wsServerCommunicationClient;
    }

    public ServerSelector getServerSelector() {
        return serverSelector;
    }

    public void setServerSelector(ServerSelector serverSelector) {
        this.serverSelector = serverSelector;
    }

    public WsContainer getWsContainer() {
        return wsContainer;
    }
}
