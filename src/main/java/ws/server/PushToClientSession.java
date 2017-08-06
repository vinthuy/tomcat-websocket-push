package ws.server;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import ex.BizException;
import ws.Constants;
import ws.SessionSender;
import ws.WsContainerSingle;
import ws.protocol.PushData;
import ws.util.WsUtil;
import wshandle.WsConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 1.web-socket push server
 * 2.generate a instance when a web-socket was created  successfully.
 * 3.���ӵ�Э���б�����p=?����������ƽ̨����ע��,�ұ���ŵ�һ��
 */
@ServerEndpoint("/ws/pushClient.ws")
public class PushToClientSession extends SessionSender {


    private String key;

    private ByteBuffer byteBuffer;
    //��Ҫͬ������������
//    private Map<String, PushData> pushDataRequestMap;

    private Cache<String, PushData> pushDataRequestMap;

    public PushToClientSession() {
        super(WsContainerSingle.instance().getPushServer(), WsContainerSingle.instance().getSerializeManager());
//        pushDataRequestMap = new ConcurrentHashMap<String, PushData>();

        pushDataRequestMap = CacheBuilder.newBuilder()
                .maximumSize(100000)//���ô�С����Ŀ��
                .expireAfterWrite(90, TimeUnit.SECONDS)//����ʧЧʱ�䣬����ʱ��
                .expireAfterAccess(60, TimeUnit.SECONDS) //����ʱЧʱ�䣬���һ�α�����
                .build();
    }


    @OnOpen
    public void onOpen(Session session) {
        Constants.wslogger.warn("server Session [id=" + session.getId() + "] is connect successfully");
        this.session = session;
        session.setMaxTextMessageBufferSize(8192 * 10);
        session.setMaxIdleTimeout(120 * 1000);
        String queryString = session.getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            String[] queryArray = queryString.split("=");
            if (queryArray.length > 1) {
                String p = queryArray[0];
                if (p.equalsIgnoreCase("p")) {
                    String key = queryString.split("=")[1];
                    if (StringUtils.isNotBlank(key)) {
                        if (WsConfig.PushServerKey.get(key) != null) {
                            this.key = key;
                            byteBuffer = ByteBuffer.allocate(Constants.reponseHeartBytes.length);
                            pushServer.addPushToClientSession(key, this);
                            pushServer.getServerSessionListener().onStart(this);
                            return;
                        }
                    }
                }
            }

        }
        CloseReason.CloseCode closeCode = new CloseReason.CloseCode() {

            @Override
            public int getCode() {
                return -100;
            }
        };
        try {
            session.close(new CloseReason(closeCode, "Not support q = "));
        } catch (IOException error) {
            Constants.wslogger.error("server Session[id=" + session.getId() + "] has error:" + error.getMessage(), error);
        }


    }

    @OnClose
    public void onClose() {
        Constants.wslogger.warn("server Session [id=" + session.getId() + "] is closed!!!!");
        pushServer.removePushToClientSession(key, this);
        pushServer.getServerSessionListener().onClose(this);
        byteBuffer = null;
        pushDataRequestMap = null;
    }

    @OnMessage
    public void processFragment(byte[] responseData, boolean isLast, Session session) {
        //��������
        byte[] heartBytes = Constants.requestHeartBytes;
        if (ArrayUtils.isEquals(heartBytes, responseData)) {
            heartResponse();
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
        PushData pushData = pushDataRequestMap.getIfPresent(requestId);
        if (pushData == null) {
            Constants.wslogger.debug("processFragment not find pushData then discard!!");
            return;
        }
        pushData.addData(sendDataFrame.getData());
        if (isLast) {
            if (Constants.wslogger.isDebugEnabled()) {
                Constants.wslogger.debug("receive:" + ArrayUtils.toString(pushData.getPushResponse()));
            }
            pushData.done();
        } else {
            // there is more to come;
        }
    }

    public void heartResponse() {
        byteBuffer.clear();
        byteBuffer.put(Constants.reponseHeartBytes);
        byteBuffer.flip();
        try {
            session.getBasicRemote().sendBinary(byteBuffer);
        } catch (IOException e) {
            try {
                session.close();
            } catch (IOException e1) {

            }
        }
    }

    /**
     * ���ܵ��ͻ�����Ϣ�Ĵ���
     * <p>
     * //     * @param message
     *
     * @param session
     */
//    @OnMessage
//    public void onMessage(String message, Session session) {
//        //�߼�����ά��
//        if (message.equalsIgnoreCase(Constants.requestHeart)) {
//            try {
//                session.getBasicRemote().sendText(Constants.responsetHeart);
//            } catch (IOException e) {
//
//            }
//        } else {
//            Constants.wslogger.info("server receive the message:" + message);
//        }
//    }
    @OnError
    public void onError(Session session, Throwable error) {
        Constants.wslogger.error("server Session[id=" + session.getId() + "] has error:" + error.getMessage(), error);
        pushServer.getServerSessionListener().onError(this);
    }

    //���������ı���Ϣ,���Գ����һ��Э��
    public <RS> RS sendMessage(final Object message, boolean sync) throws Exception {
        PushData pushData = new PushData(this, message);
        handlePushData(pushData);
        //������ɺ������������
        pushData.setSendData(null);

        if (sync) {
            //�ȴ��������
            pushDataRequestMap.put(pushData.getRequestId(), pushData);
            try {
                Object res = pushData.get(WsContainerSingle.instance().getWsConfigDO().getPushDataTimeout());
                if (res != null) {
                    return (RS) serializeManager.deserialize((byte[]) res);
                }
            } finally {
                if (pushData.getRequestId() != null) {
                    pushDataRequestMap.invalidate(pushData.getRequestId());
                }
            }
        } else {
            //���Ͳ����������,ֱ�ӷ��ͺ����Ϊ�Ѿ����
            pushData.done();
        }
        return null;
    }


    public void handlePushData(PushData pushData) throws IOException {
        Object sendData = pushData.getSendData();
        byte[] requestData = null;
        if (this.session.isOpen()) {
            try {
                requestData = serializeManager.serialize(sendData);
            } catch (Exception e) {
                Constants.wslogger.error("push message error >>>>" + e.getMessage(), e);
            }
            if (requestData == null || requestData.length == 0) {
                return;
            }
            send(pushData.getRequestId(), requestData);
        } else {
            this.session.close();
            onClose();
            Constants.wslogger.error("push message error >>>>");
            throw new BizException("���ͷ���ػ��ر�,���Ժ�����");
        }
        //this.session.getAsyncRemote().sendText(message);
    }


    public void sendMessage(String message) throws IOException {
        if (this.session.isOpen()) {
//            ������ݹ���,ͨ��������Ϸ�ʽ����
            if (message.length() > WsContainerSingle.instance().getWsConfigDO().getPullStrLength()) {
                MsgForwarder msgForwarder = pushServer.getMsgForwarder();
                if (msgForwarder == null) {
                    throw new RuntimeException("Not find msgForwarder.");
                }
                String msgKey = msgForwarder.putMsg(message);
                String pullMsgHeader = WsUtil.genPullMsgHeader(msgKey);
                Constants.wslogger.warn("push message -->" + pullMsgHeader);
                this.session.getBasicRemote().sendText(pullMsgHeader);
            } else {
                Constants.wslogger.warn("push message -->" + message);
                this.session.getBasicRemote().sendText(message);
            }
        } else {
            try {
                this.session.close();
            } catch (Exception e) {
                //
            }
            Constants.wslogger.error("push message error >>>>");
            throw new BizException("���ͷ��������ر�,������");
        }
        //this.session.getAsyncRemote().sendText(message);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PushToClientSession)) return false;
        PushToClientSession that = (PushToClientSession) o;
        return Objects.equals(session, that.session);
    }

    @Override
    public int hashCode() {
        return Objects.hash(session);
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Session getSession() {
        return session;
    }

    @Override
    public String toString() {
        if (session != null)
            return com.google.common.base.Objects.toStringHelper(this)
                    .add("key", key).add("sessionId", session.getId())
                    .toString();
        return "sessionNull";
    }


    public PushServer getPushServer() {
        return pushServer;
    }
}