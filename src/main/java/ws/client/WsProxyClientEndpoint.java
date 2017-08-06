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
 * websocket����ͻ���
 * �������Է���˵�������Ϣ
 *
 * @author ruiyong.hry
 */
@ClientEndpoint
public class WsProxyClientEndpoint extends SessionSender {


    //��Ҫͬ������������
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
                .maximumSize(100000)//���ô�С����Ŀ��
                .expireAfterWrite(90, TimeUnit.SECONDS)//����ʧЧʱ�䣬����ʱ��
                .expireAfterAccess(60, TimeUnit.SECONDS) //����ʱЧʱ�䣬���һ�α�����
                .build();
    }

    @OnClose
    public void onClose() {
        Constants.wslogger.warn("Client Session [id=" + session.getId() + "] is closed!!!!");
        //�Զ�����
        WsProxyClient wsProxyClient = WsContainerSingle.instance().getWsProxyClient(session);
        if (wsProxyClient != null) {
            Constants.wslogger.warn("Session reconnecting");
            wsProxyClient.newSession();
        }
    }

    /**
     * �����Ϣ�Ƿ�������͵�.
     * ���ڿͻ������ǲɼ����̴߳���
     * <p>
     * //     * @param message
     */
//    @OnMessage
//    public void processMessage(String message, Session session) {
//        if (!message.equalsIgnoreCase(Constants.responsetHeart)) {
//            WsContainer.instance().getClientThreadPool().submit(new WsProxyClientTask(message, session));
//        }
//    }
    // ��������Ϣ�߼�ѡ��������ն���������
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
                //�����Ҫ��������
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
                        //��Ϣ���Ϸ��󷽶���
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
//                //1.�����ж��Ƿ�����Ҫ��ȡ��Ϣ
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
//                //��Ϣ���Ϸ��󷽶���
//                Constants.wslogger.error("Handle message failed!!->" + e.getMessage(), e);
//            }
//
//        }
//    }

}