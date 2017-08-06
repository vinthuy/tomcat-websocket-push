package ws.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import ws.Constants;
import ws.SessionSender;
import ws.WsContainerSingle;
import ws.protocol.PushData;
import ws.protocol.WsTsPortHandle;
import ws.util.WsUtil;
import org.apache.commons.lang.ArrayUtils;

import javax.websocket.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * websocket代理客户端
 * 接受来自服务端的推送消息
 *
 * @author ruiyong.hry
 */
@ClientEndpoint
public class WsProxyClientEndpoint extends SessionSender {


    //需要同步的请求数据
//    private Map<String, ConcurrentLinkedDeque<byte[]>> pushDataRequestMap;
    private Cache<String, ConcurrentLinkedDeque<byte[]>> pushDataRequestMap;

    public WsProxyClientEndpoint() {
        super(WsContainerSingle.instance().getPushServer(), WsContainerSingle.instance().getSerializeManager());
    }

    @OnOpen
    public void onOpen(Session session) {
        Constants.wslogger.warn("Client Session[id=" + session.getId() + "] is connected");
        this.session = session;
//        pushDataRequestMap = new ConcurrentHashMap<String, ConcurrentLinkedDeque<byte[]>>();
        pushDataRequestMap = CacheBuilder.newBuilder()
                .maximumSize(100000)//设置大小，条目数
                .expireAfterWrite(90, TimeUnit.SECONDS)//设置失效时间，创建时间
                .expireAfterAccess(60, TimeUnit.SECONDS) //设置时效时间，最后一次被访问
                .build();
    }

    @OnClose
    public void onClose() {
        Constants.wslogger.warn("Client Session [id=" + session.getId() + "] is closed!!!!");
        //自动重连
        WsProxyClient wsProxyClient = WsContainerSingle.instance().getWsProxyClient(session);
        if (wsProxyClient != null) {
            Constants.wslogger.warn("Session reconnecting");
            wsProxyClient.newSession();
        }
    }

    /**
     * 这个消息是服务端推送的.
     * 对于客户端我们采集多线程处理
     * <p>
     * //     * @param message
     */
//    @OnMessage
//    public void processMessage(String message, Session session) {
//        if (!message.equalsIgnoreCase(Constants.responsetHeart)) {
//            WsContainer.instance().getClientThreadPool().submit(new WsProxyClientTask(message, session));
//        }
//    }
    // 二进制消息高级选项：分批接收二进制数据
    @OnMessage
    public void processFragment(byte[] responseData, boolean isLast, Session session) {
        byte[] heartBytes = Constants.reponseHeartBytes;
        if (ArrayUtils.isEquals(heartBytes, responseData)) {
            return;
        }

        PushData.SendDataFrame sendDataFrame = null;
        try {
            sendDataFrame = (PushData.SendDataFrame) serializeManager.deserialize(responseData);
        } catch (Exception e) {
            Constants.wslogger.error("processFragment error >>>>" + e.getMessage(), e);
        }

        if (sendDataFrame == null) {
            return;
        }

        String requestId = sendDataFrame.getRequestId();

        ConcurrentLinkedDeque<byte[]> pushRequestData = pushDataRequestMap.getIfPresent(requestId);
        if (!isLast) {
            if (pushRequestData == null) {
                pushRequestData = new ConcurrentLinkedDeque<byte[]>();
                pushDataRequestMap.put(requestId, pushRequestData);
            }
            pushRequestData.add(sendDataFrame.getData());
        }
        if (isLast) {
            try {
                byte[] requestDataByte = null;
                if (pushRequestData == null) {
                    requestDataByte = sendDataFrame.getData();
                } else {
                    if (pushRequestData == null) {
                        Constants.wslogger.error("package is dropped!requestId=" + requestId);
                        return;
                    }
                    pushRequestData.add(sendDataFrame.getData());
                    requestDataByte = WsUtil.joinByte(pushRequestData);
                }
                handleRequest(requestId, requestDataByte);
            } finally {
                //完成需要清理数据
                if (pushRequestData != null) {
                    pushDataRequestMap.invalidate(requestId);
                }
            }
        }
    }


    @OnError
    @Override
    public void onError(Session session, Throwable t) {
        Constants.wslogger.error("WsProxyClientEndpoint error: " + t.getMessage(), t);
    }

    private void handleRequest(String requestId, byte[] requestDataByte) {
        if (!ArrayUtils.isEmpty(requestDataByte)) {
            if (Constants.wslogger.isDebugEnabled()) {
                Constants.wslogger.debug("handle request:" + ArrayUtils.toString(requestDataByte));
            }
            try {
                Object requestData = serializeManager.deserialize(requestDataByte);
                if (requestData != null) {
                    try {
                        WsTsPortHandle wsTsPortHandle = WsContainerSingle.instance().getClientWsTsPortHandle();
                        Object res = wsTsPortHandle.handle(requestData);
                        if (res != null) {
                            byte[] responseData = serializeManager.serialize(res);
                            if (responseData != null && responseData.length > 0) {
                                send(requestId, responseData);
                            }
                        }
                    } catch (Exception e) {
                        //消息不合符贵方丢弃
                        Constants.wslogger.error("Handle message failed!!->" + e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                Constants.wslogger.error("handleRequest error:" + e.getMessage(), e);
            }
        }
    }


//    static class WsProxyClientTask implements Runnable {
//
//        private String message;
//        private Session session;
//
//        public WsProxyClientTask(String message, Session session) {
//            this.message = message;
//            this.session = session;
//        }
//
//        public void run() {
//            try {
//                //1.首先判断是否是需要拉取消息
//                if (WsUtil.isPullMsgHeader(message)) {
//                    PullMsg pullMsg = WsUtil.splitMsgKey(message);
//                    if (pullMsg == null) {
//                        Constants.wslogger.error("pull msg is err.");
//                    }
//                    WsPull wsPull = WsContainer.instance().getWsPull();
//                    if (wsPull == null) {
//                        Constants.wslogger.error("Not support pull big data");
//                    }
//                    message = wsPull.pull(pullMsg);
//                    if (StringUtils.isBlank(message)) {
//                        Constants.wslogger.error("pull msg is null or error");
//                        return;
//                    }
//                }
//
//
//                WsResult wsResult = JSON.parseObject(message, WsResult.class);
//                if (wsResult.isSuccess()) {
//                    WsResultHandler wsResultHandler = WsContainer.instance().findWsResultHandler(wsResult.getFlag());
//                    if (wsResultHandler != null) {
//                        WsResult wsrsp = wsResultHandler.handle(wsResult);
//                        if (wsrsp != null) {
//                            WsProxyClient wsProxyClient = WsContainer.instance().getWsProxyClient(session);
//                            if (wsProxyClient != null) {
//                                wsProxyClient.SendWsResultProtocol(wsrsp);
//                            }
//                        }
//                    } else {
//                        Constants.wslogger.warn("message is dropped:" + message);
//                    }
//                }
//            } catch (Exception e) {
//                //消息不合符贵方丢弃
//                Constants.wslogger.error("Handle message failed!!->" + e.getMessage(), e);
//            }
//
//        }
//    }

}