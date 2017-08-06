package ws.server;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ws.Constants;
import ws.ex.BizException;
import ws.model.EnvCacheValue;
import ws.model.ServerEndpoint;
import ws.protocol.SenderApi;
import ws.server.ext.WsServerInterResult;
import ws.util.EnvUtil;
import ws.util.HostServerUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * push server.
 * Created by ruiyong.hry on 02/07/2017.
 */
public class PushServer {


    private Selectetor selector;

    private WebSocketClientListener webSocketClientListener;

    //缓存proxyClientListeners的大小
    private ConcurrentHashMap<String, ConcurrentSkipListMap<String, PushToClientSession>> proxyClientListenersMap;


    //pushtoSession listener
    private ServerSessionListener serverSessionListener;

    private SessionManager sessionManager;

    private WsServerCommunicationClient wsServerCommunicationClient;

    //寻找session 重试次数 (如果分布式机器数量较少,一般可为机器减去1)
    private int retryCountWhenSessionServerIpFail = 10;

    private MsgForwarder msgForwarder;

    private SenderApi senderApi;

    public PushServer() {
        proxyClientListenersMap = new ConcurrentHashMap<String, ConcurrentSkipListMap<String, PushToClientSession>>();
        selector = new SelectetorImpl();
        webSocketClientListener = new EmptyWebSocketClientListener();
    }

    public boolean addPushToClientSession(String key, PushToClientSession pushToClientListener) {
        ConcurrentSkipListMap<String, PushToClientSession> list = proxyClientListenersMap.get(key);
        if (list == null) {
            list = new ConcurrentSkipListMap<String, PushToClientSession>();
            proxyClientListenersMap.put(key, list);
        }
        try {
            if (pushToClientListener.getSession() != null) {
                list.put(pushToClientListener.getSession().getId(), pushToClientListener);
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
                    list.remove(pushToClientListener.getSession().getId());
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    public <RS> RS sendText(String key, Object message, MsgParse msgParse, boolean sync) {
        String txt = msgParse.parse(message);
        if (StringUtils.isBlank(txt)) {
            Constants.wslogger.warn("Not find wsClient,message = [" + message + "] dropped");
            return null;
        }
        String text = msgParse.parse(message);
        return sendObj(key, text, sync);
    }

    public <RS> RS sendObj(String key, Object message, boolean sync) {
        //test--code
//        WsServerInterResult wsServerInterResult = null;
//        try {
//            wsServerInterResult = wsServerCommunicationClient.send(key, message, HostServerUtil.getLocalIp(), HostServerUtil.getPort(), sync);
//        } catch (Exception e) {
//            if (e instanceof BizException) {
//                throw (BizException) e;
//            }
//            Constants.wslogger.error("wsServerCommunicationClient.request.error->" + e.getMessage(), e);
//        }
//
//        if (wsServerInterResult != null) {
//            return (RS) wsServerInterResult.getData();
//        }
//        return null;
//      return   executeSendInOtherServerIp(key, message, sync);
        //-----正常流程
        ConcurrentSkipListMap<String, PushToClientSession> list = proxyClientListenersMap.get(key);
        if (MapUtils.isEmpty(list)) {
            //找不到就调用api 找到可用的serverIP,进行调用
            return executeSendInOtherServerIp(key, message, sync);
        }
        return executeSendLocal(key, message, sync);
    }


//    /**
//     * 本地执行推送
//     */
//    public <RS> RS executeSendLocal(String key, String txt) {
//        return executeSendLocal(key, txt, false);
//    }

    public <RS> RS executeSendLocal(String key, Object msg, boolean sync) {
        ConcurrentSkipListMap<String, PushToClientSession> list = proxyClientListenersMap.get(key);
        if (MapUtils.isEmpty(list)) {
            Constants.wslogger.warn("Not find useful push-to-session");
            throw new BizException("推送客户端发生错误,请在30s秒后重试!");
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
                Constants.wslogger.warn("Not find wsClient,message = [" + msg + "] dropped");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            Constants.wslogger.error("send text error:" + e.getMessage(), e);
            throw new BizException("推送客户端发生错误,请在30s秒后重试!");
        }
        return null;
    }

    /**
     * 在其他机器上执行推送
     *
     * @param key
     * @return
     */
    private <RS> RS executeSendInOtherServerIp(String key, Object message, boolean sync) {
        List<EnvCacheValue> envCacheValues = getSessionManager().getAllSessionCount(key);
        if (envCacheValues == null) {
            Constants.wslogger.error(String.format(" %s :%s not find allsession", HostServerUtil.getLocalIp(), HostServerUtil.getPort()));
            throw new BizException("没有推送客户端连接,请在30s秒后重试!");
        }

        WsServerInterResult<RS> wsServerInterResult = null;
        for (int i = 0; i < retryCountWhenSessionServerIpFail; i++) {
            ServerEndpoint serverEnpoint = findCanUsefulSessionServer(key, envCacheValues);

            Constants.wslogger.warn(String.format("executeSendInOtherServerIp find %s ", serverEnpoint));

            if (serverEnpoint == null) {
                throw new BizException("没有推送客户端连接,请在30s秒后重试!");
            }

            try {
                wsServerInterResult = wsServerCommunicationClient.send(key, message, serverEnpoint.getServerIp(), serverEnpoint.getPort(), sync);
            } catch (Exception e) {
                if (e instanceof BizException) {
                    throw (BizException) e;
                }
                Constants.wslogger.error("wsServerCommunicationClient.request.error->" + e.getMessage(), e);
            }

            if (wsServerInterResult == null) {
                throw new BizException("解析远程推送结果发生错误,请在30s秒后重试!,serverEndpoint=" + serverEnpoint);
            }

            //如果请求成功,那么结束了,否则会重试
            if (wsServerInterResult.isSuccess()) {
                return wsServerInterResult.getData();
            }
            //走到这儿,说明没有成功,删除再来
            for (int j = envCacheValues.size() - 1; j >= 0; j--) {
                EnvCacheValue envCacheValue = envCacheValues.get(j);
                if (envCacheValue == null || ObjectUtils.equals(envCacheValue.getServerEndpoint(), serverEnpoint)) {
                    envCacheValues.remove(j);
                }
            }
        }
        if (wsServerInterResult != null && !wsServerInterResult.isSuccess()) {
            throw new BizException("路由错误" + wsServerInterResult.getMsg());
        }
        return null;
    }

    private ServerEndpoint findCanUsefulSessionServer(String key, List<EnvCacheValue> envCacheValues) {
        if (CollectionUtils.isNotEmpty(envCacheValues)) {
            Map<ServerEndpoint, Integer> serverSessionCountMap = new HashMap<ServerEndpoint, Integer>();
            for (EnvCacheValue envCacheValue : envCacheValues) {
                //不同环境排除
                if (envCacheValue.getServerEnv() != EnvUtil.getEnv()) {
                    continue;
                }
                //自身ip排除
                ServerEndpoint serverEnpoint = envCacheValue.getServerEndpoint();
                if (ObjectUtils.equals(serverEnpoint.getServerIp(), HostServerUtil.getLocalIp()) &&
                        ObjectUtils.equals(serverEnpoint.getPort(), HostServerUtil.getPort())) {
                    continue;
                }
                serverSessionCountMap.put(serverEnpoint, envCacheValue.getSessionCount());
//                Integer count = serverSessionCountMap.get(serverEnpoint);
//                if (count == null) {
//                    serverSessionCountMap.put(serverEnpoint, 1);
//                } else {
//                    count++;
//                    serverSessionCountMap.put(serverEnpoint, count);
//                }
            }
            ServerEndpoint maxServerIpKey = null;
            Integer maxSessionCount = 0;
            for (Map.Entry<ServerEndpoint, Integer> serverSessionCountEntry : serverSessionCountMap.entrySet()) {
                if (serverSessionCountEntry.getValue() > maxSessionCount) {
                    maxSessionCount = serverSessionCountEntry.getValue();
                    maxServerIpKey = serverSessionCountEntry.getKey();
                }
            }
            return maxServerIpKey;
        }
        return null;
    }

    /**
     * 同步push-to-client到cache
     */
    public void synchronizedPushToClientSessionToCaChe() {
        if (MapUtils.isNotEmpty(proxyClientListenersMap)) {
            Constants.wslogger.warn("synchronizedPushToClientSessionToCaChe starting ....");
            for (Map.Entry<String, ConcurrentSkipListMap<String, PushToClientSession>> proxyClientListenersEntry : proxyClientListenersMap.entrySet()) {
                //直接同步个数
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

                Constants.wslogger.warn(String.format("synchronizedPushToClientSessionToCaChe key:%s cacheSessionCount:%s->realSessionCount:%s", new Object[]{key, cacheSessionCount, realSessionCount}));
                //不相同就同步
                if (cacheSessionCount != realSessionCount) {
                    sessionManager.setSessionCount(key, realSessionCount);
                }


//                if (envCacheValues == null) {
//                    envCacheValues = Lists.newArrayList();
//                }
//
//                //复制一份
//                List<PushToClientSession> realSessions;
//                if (values == null) {
//                    realSessions = Lists.newArrayList();
//                } else {
//                    realSessions = Lists.newArrayList(values.values());
//                }
//
//
//                //2.求cache需要增加和删除的的session
//                List<EnvCacheValue> needAddSessions = Lists.newArrayList();
//                List<EnvCacheValue> needRemoveSessions = Lists.newArrayList();
//
//                Constants.wslogger.warn(String.format("synchronizedPushToClientSessionToCaChe realSessions:%s ,envCacheValues:%s ", realSessions, envCacheValues));
//
//
//                Iterator<PushToClientSession> pushToClientSessionIterator = realSessions.iterator();
//                while (pushToClientSessionIterator.hasNext()) {
//                    PushToClientSession pushToClientSession = pushToClientSessionIterator.next();
//                    if (pushToClientSession.getSession() != null) {
//                        Iterator<EnvCacheValue> envCacheValueIterable = envCacheValues.iterator();
//                        while (envCacheValueIterable.hasNext()) {
//                            EnvCacheValue envCacheValue = envCacheValueIterable.next();
//                            if (ObjectUtils.equals(envCacheValue.getSessionId(), pushToClientSession.getSession().getId())) {
//                                //相等,则移除
//                                envCacheValueIterable.remove();
//                                pushToClientSessionIterator.remove();
//                                break;
//                            }
//                        }
//                    }
//                }//内部循环走完后,在外部循环
//
//
//                //最后剩下的就是我们想要的结果.
//                if (realSessions != null) {
//                    for (PushToClientSession session : realSessions) {
//                        if (session.getSession() != null) {
//                            EnvCacheValue envCacheValue = new EnvCacheValue();
//                            envCacheValue.setSessionId(session.getSession().getId());
//                            ServerEndpoint serverEndpoint = new ServerEndpoint(HostServerUtil.getLocalIp(), HostServerUtil.getPort());
//                            envCacheValue.setServerEndpoint(serverEndpoint);
//                            envCacheValue.setServerEnv(EnvUtil.getEnv());
//                            needAddSessions.add(envCacheValue);
//                        }
//                    }
//                }
//
//                //需要删除的
//                if (envCacheValues != null) {
//                    for (EnvCacheValue envCacheValue : envCacheValues) {
//                        needRemoveSessions.add(envCacheValue);
//                    }
//                }
//
//                Constants.wslogger.warn(String.format("synchronizedPushToClientSessionToCaChe addSession:%s ,removeCache:%s ", needAddSessions, needRemoveSessions));
//                for (EnvCacheValue envCacheValue : needAddSessions) {
//                    sessionManager.addSession(key, envCacheValue.getSessionId());
//                }
//
//                for (EnvCacheValue envCacheValue : needRemoveSessions) {
//                    sessionManager.removeSession(key, envCacheValue);
//                }
            }
            Constants.wslogger.warn("[synchronizedPushToClientSessionToCaChe] end ....");
        }
    }

    public <RS> void SendWsResultProtocol(String clientKey, RS rs) {
        senderApi.SendWsResultProtocol(clientKey, rs, false);
    }

    public <RS> RS SendWsResultProtocol(String clientKey, RS rs, boolean sync) {
        return (RS) senderApi.SendWsResultProtocol(clientKey, rs, sync);
    }


    public interface MsgParse {
        public String parse(Object msg);
    }


    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public ServerSessionListener getServerSessionListener() {
        return serverSessionListener;
    }

    public void setServerSessionListener(ServerSessionListener serverSessionListener) {
        this.serverSessionListener = serverSessionListener;
    }

    public void setRetryCountWhenSessionServerIpFail(int retryCountWhenSessionServerIpFail) {
        this.retryCountWhenSessionServerIpFail = retryCountWhenSessionServerIpFail;
    }

    public MsgForwarder getMsgForwarder() {
        return msgForwarder;
    }

    public void setMsgForwarder(MsgForwarder msgForwarder) {
        this.msgForwarder = msgForwarder;
    }

    public Map<String, List<String>> dumpSession() {
        Map<String, List<String>> map = Maps.newHashMap();
        for (Map.Entry<String, ConcurrentSkipListMap<String, PushToClientSession>> proxyClientListenersEntry : proxyClientListenersMap.entrySet()) {
            String key = proxyClientListenersEntry.getKey();
            List<String> list = Lists.newArrayList(proxyClientListenersEntry.getValue().descendingKeySet());
            map.put(key, list);
        }
        return map;
    }


    //推送ID生成器
    private AtomicLong PushGenID = new AtomicLong(0);


    public long genPushDataId() {
        return PushGenID.incrementAndGet();
    }

    public SenderApi getSenderApi() {
        return senderApi;
    }

    public void setSenderApi(SenderApi senderApi) {
        this.senderApi = senderApi;
    }

    public WsServerCommunicationClient getWsServerCommunicationClient() {
        return wsServerCommunicationClient;
    }

    public void setWsServerCommunicationClient(WsServerCommunicationClient wsServerCommunicationClient) {
        this.wsServerCommunicationClient = wsServerCommunicationClient;
    }
}
