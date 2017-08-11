package ws.protocol;

import ws.WsContainerSingle;
import ws.session.WsSessionAPI;
import ws.util.WsUtil;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 此类用于实现网络的同步
 * Created by ruiyong.hry on 03/08/2017.
 */
public class PushData<RE> implements PushDataFurture {

    private volatile boolean done;

    private ConcurrentLinkedDeque<byte[]> pushResponse;

    private String requestId;

    //    private Object sendData;
    private int maxWaiteDefault = 15 * 1000;

    private int intervelWait = 3000;

    private RE sendData;


    public PushData(WsSessionAPI sessionAPI, RE _sendData) {
        genRequestId(sessionAPI.id(), WsContainerSingle.instance().genPushDataId());
        sendData = _sendData;
        done = false;
        pushResponse = new ConcurrentLinkedDeque<byte[]>();
    }

    public void genRequestId(String sessionId, long pushDataId) {
        requestId = sessionId + "@" + pushDataId;
    }

    @Override
    public String requestId() {
        return this.requestId;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public void done() {
        this.done = true;
    }

    @Override
    public void doneFroSync() {
        synchronized (this) {
            done();
            notifyAll();
        }
    }

    @Override
    public Object get() throws InterruptedException {
        return get(maxWaiteDefault);
    }

    @Override
    public Object get(long timeout) throws InterruptedException {
        if (timeout < 0) {
            synchronized (this) { // 旋锁
                while (!done) {
                    wait();
                }
            }
        } else if (timeout > 0) {
            long end = System.currentTimeMillis() + timeout;
            long waitTime = intervelWait;
            synchronized (this) {
                while (!done && waitTime > 0) {
                    wait(waitTime);
                    waitTime = end - System.currentTimeMillis();
                }
            }
        }

        if (!done) {
            return null;
        }
        return getPushResponse();
    }


    public byte[] getPushResponse() {
        if (pushResponse != null) {
            return WsUtil.joinByte(pushResponse);
        }
        return null;
    }


    //SendData 拆分出的数据
    public static class SendDataFrame {
        protected String requestId;
        //        protected int index;
        protected byte[] data;

        protected byte msgType;

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

//        public int getIndex() {
//            return index;
//        }
//
//        public void setIndex(int index) {
//            this.index = index;
//        }

        public byte getMsgType() {
            return msgType;
        }

        public void setMsgType(byte msgType) {
            this.msgType = msgType;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }


    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void addData(byte[] resData) {
        pushResponse.add(resData);
    }

    public RE getSendData() {
        return sendData;
    }

    public void setSendData(RE sendData) {
        this.sendData = sendData;
    }
}
