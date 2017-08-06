package ws;

import ws.protocol.PushData;
import ws.serialize.SerializeManager;
import ws.server.PushServer;

import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ruiyong.hry on 04/08/2017.
 */
public abstract class SessionSender {

    protected PushServer pushServer;

    protected SerializeManager serializeManager;

    protected Session session;


    public SessionSender(PushServer pushServer, SerializeManager serializeManager) {
        this.pushServer = pushServer;
        this.serializeManager = serializeManager;
    }

    public void send(String requestId, byte[] requestData) throws IOException {
        //            ������ݹ���,ͨ��������Ϸ�ʽ����
        int limit = WsContainerSingle.instance().getWsConfigDO().getPullStrLength();
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

            byte[] frameData = null;
            try {
                frameData = serializeManager.serialize(sendDataFrame);
            } catch (Exception e) {
                Constants.wslogger.error("pushData serialize error:" + e.getMessage(), e);
            }
            if (frameData != null) {
                ByteBuffer buffer = ByteBuffer.wrap(frameData);
//                buffer.clear();
//                buffer.put(frameData);
//                buffer.flip();

                if (i == page) { //���һ�η���
                    session.getBasicRemote().sendBinary(buffer, true);
                } else {
                    session.getBasicRemote().sendBinary(buffer, false);
                }
            }
        }

    }


    public abstract void onOpen(Session session);

    public abstract void onClose();

    public abstract void processFragment(byte[] responseData, boolean isLast, Session session);

    public abstract void onError(Session session, Throwable error);



}
