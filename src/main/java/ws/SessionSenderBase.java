package ws;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import ws.protocol.PushData;
import ws.protocol.WsTsPortHandle;
import ws.serialize.SerializeManager;
import ws.session.WsSessionAPI;
import ws.util.WsUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public abstract class SessionSenderBase {

    /**
     * ����Է������Ʒ��ؽ����ͬ������
     */
    protected Cache<String, PushData> pushDataRequestMap;

    //������Ҫͬ������������
    protected Cache<String, ConcurrentLinkedDeque<byte[]>> pushDataRequestSyncDataMap;

    public SessionSenderBase() {
        pushDataRequestMap = CacheBuilder.newBuilder()
                .maximumSize(100000)//���ô�С����Ŀ��
                .expireAfterWrite(90, TimeUnit.SECONDS)//����ʧЧʱ�䣬����ʱ��
                .expireAfterAccess(60, TimeUnit.SECONDS) //����ʱЧʱ�䣬���һ�α�����
                .build();

        pushDataRequestSyncDataMap = CacheBuilder.newBuilder()
                .maximumSize(100000)//���ô�С����Ŀ��
                .expireAfterWrite(90, TimeUnit.SECONDS)//����ʧЧʱ�䣬����ʱ��
                .expireAfterAccess(60, TimeUnit.SECONDS) //����ʱЧʱ�䣬���һ�α�����
                .build();
    }

    public void send(String requestId, byte[] requestData, byte msgType) throws IOException {
        //            ������ݹ���,ͨ��������Ϸ�ʽ����
        int limit = WsContainerSingle.instance().getWsConfigDO().getPullStrOrSplitAtLength();
        //���
        int page = (requestData.length - 1) / limit; //13/3 = 4 ѭ������page+1 10/5 = 2 ѭ����������2
        int remainSize = requestData.length % limit;

        PushData.SendDataFrame sendDataFrame = null;


        int start = 0;
        for (int i = 0; i <= page; i++) {
            byte[] byteTmp;
            //���ֻ��һ��ѭ��,˵�����ò��,����new byte[]����
            if (page == 0) {
                byteTmp = requestData;
            } else {
                start = i * limit;
                if (i == page && remainSize > 0) {
                    //���һ��ѭ��
                    byteTmp = new byte[remainSize];
                    System.arraycopy(requestData, start, byteTmp, 0, remainSize);
                } else {
                    byteTmp = new byte[limit];
                    System.arraycopy(requestData, start, byteTmp, 0, limit);
                }
            }

            sendDataFrame = new PushData.SendDataFrame();
            sendDataFrame.setRequestId(requestId);
//            sendDataFrame.setIndex(i);
            sendDataFrame.setData(byteTmp);
            sendDataFrame.setMsgType(msgType);
            byte[] frameData = null;
            try {
                frameData = serializeManager().serialize(sendDataFrame);
            } catch (Exception e) {
                WsConstants.wslogger.error("pushData serialize error:" + e.getMessage(), e);
            }
            if (frameData != null) {
                boolean first = (i == 0);

                if (i == page) { //���һ�η���
                    wsSessionAPI().sendBinary(frameData, first, true);
                } else {
                    wsSessionAPI().sendBinary(frameData, first, false);
                }
            }
        }

    }

    protected abstract WsSessionAPI wsSessionAPI();

    protected abstract SerializeManager serializeManager();


    protected abstract WsTsPortHandle wsTsPortHandle();


    protected String getParam(Map<String, List<String>> param, String key) {
        if (param != null) {
            List<String> values = param.get(key);
            if (CollectionUtils.isNotEmpty(values)) {
                return values.get(0);
            }
        }
        return StringUtils.EMPTY;
    }


    //���������ı���Ϣ,���Գ����һ��Э��
    public <RS> RS sendMessage(final Object message, boolean sync) throws Exception {
        PushData pushData = new PushData(wsSessionAPI(), message);
        handlePushData(pushData);
        //������ɺ������������
        pushData.setSendData(null);

        if (sync) {
            //�ȴ��������
            if (pushDataRequestMap != null) {
                pushDataRequestMap.put(pushData.getRequestId(), pushData);
            }
            try {
                Object res = pushData.get(WsContainerSingle.instance().getWsConfigDO().getPushDataTimeout());

                if (res != null) {
                    return (RS) serializeManager().deserialize((byte[]) res);
                }
            } finally {
                if (pushDataRequestMap != null) {
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
        if (wsSessionAPI().isOpen()) {
            try {
                requestData = serializeManager().serialize(sendData);
            } catch (Exception e) {
                WsConstants.wslogger.error("push message error >>>>" + e.getMessage(), e);
            }
            if (requestData == null || requestData.length == 0) {
                return;
            }
            send(pushData.getRequestId(), requestData, WsConstants.MSG_TYPE_HANDLE_RETURN);
        } else {
            wsSessionAPI().close();
            fireOnCloseEvent();
            WsConstants.wslogger.error("push message error >>>>");
            throw new WsException("���ͷ���ػ��ر�,���Ժ�����");
        }
        //this.session.getAsyncRemote().sendText(message);
    }

    protected abstract void fireOnCloseEvent();


    /**
     * �߼������ͺ�Է��ŻصĽ��
     *
     * @param isLast
     */
    private void handleReqResponseNoReturn(PushData.SendDataFrame sendDataFrame, boolean isLast) {

        String requestId = sendDataFrame.getRequestId();
        PushData pushData = pushDataRequestMap.getIfPresent(requestId);
        if (pushData == null) {
            WsConstants.wslogger.debug("processFragment not find pushData then discard!!");
            return;
        }
        pushData.addData(sendDataFrame.getData());
        if (isLast) {
            if (WsConstants.wslogger.isDebugEnabled()) {
                WsConstants.wslogger.debug("receive:" + ArrayUtils.toString(pushData.getPushResponse()));
            }
            pushData.doneFroSync();
        } else {
            // there is more to come;
        }
    }

    /**
     * �ͻ��˺ͷ���˴�����ܵ�����
     *
     * @param data   requestData,responseData
     * @param isLast
     * @param server
     */
    protected void processFra(byte[] data, boolean isLast, boolean server) {
        if (server) {
            byte[] heartBytes = WsConstants.reponseHeartBytes;
            if (ArrayUtils.isEquals(heartBytes, data)) {
                return;
            }
        }
        PushData.SendDataFrame sendDataFrame = null;
        try {
            sendDataFrame = (PushData.SendDataFrame) serializeManager().deserialize(data);
        } catch (Exception e) {
            WsConstants.wslogger.error("processFragment error >>>>" + e.getMessage(), e);
        }

        if (sendDataFrame == null) {
            return;
        }

        //�ж�����Ҫ�����߼����߼򵥶Է����ض���
        switch (sendDataFrame.getMsgType()) {
            case WsConstants.MSG_TYPE_HANDLE_RETURN:
                handleRequestAndReturn(sendDataFrame, isLast);
                break;
            case WsConstants.MSG_TYPE_SENDONLY:
                handleReqResponseNoReturn(sendDataFrame, isLast);
        }
    }


    /**
     * ����ҵ���߼������ؽ��
     */
    private void handleRequestAndReturn(PushData.SendDataFrame sendDataFrame, boolean isLast) {

        String requestId = sendDataFrame.getRequestId();

        ConcurrentLinkedDeque<byte[]> pushRequestData = pushDataRequestSyncDataMap.getIfPresent(requestId);
        if (!isLast) {
            if (pushRequestData == null) {
                pushRequestData = new ConcurrentLinkedDeque<byte[]>();
                pushDataRequestSyncDataMap.put(requestId, pushRequestData);
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
                        WsConstants.wslogger.error("package is dropped!requestId=" + requestId);
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

    private void handleRequest(String requestId, byte[] requestDataByte) {
        if (!ArrayUtils.isEmpty(requestDataByte)) {
            if (WsConstants.wslogger.isDebugEnabled()) {
                WsConstants.wslogger.debug("handleGroupKey request:" + ArrayUtils.toString(requestDataByte));
            }
            try {
                Object requestData = serializeManager().deserialize(requestDataByte);
                if (requestData != null) {
                    try {
                        WsTsPortHandle wsTsPortHandle = wsTsPortHandle();
                        Object res = wsTsPortHandle.handle(requestData);
                        if (res != null) {
                            byte[] responseData = serializeManager().serialize(res);
                            if (responseData != null && responseData.length > 0) {
                                send(requestId, responseData, WsConstants.MSG_TYPE_SENDONLY);
                            }
                        }
                    } catch (Exception e) {
                        //��Ϣ���Ϸ��󷽶���
                        WsConstants.wslogger.error("Handle message failed!!->" + e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                WsConstants.wslogger.error("handleRequest error:" + e.getMessage(), e);
            }
        }
    }


}
