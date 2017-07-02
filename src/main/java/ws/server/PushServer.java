package ws.server;


import ws.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * push server.
 * Created by ruiyong.hry on 02/07/2017.
 */
public class PushServer {


    private SelectStrategy selectStrategy;

    private WebSocketClientListener webSocketClientListener;

    //缓存proxyClientListeners的大小
    private List<PushToClientSession> proxyClientListeners;

    private ReadWriteLock readWriteLock;


    public PushServer() {
        readWriteLock = new ReentrantReadWriteLock();
        proxyClientListeners = new ArrayList<PushToClientSession>();
        selectStrategy = new SelectStrategyImpl();
        webSocketClientListener = new EmptyWebSocketClientListener();
    }

    public boolean addPushToClientSession(PushToClientSession pushToClientListener) {
        Lock writeLock = readWriteLock.writeLock();
        try {
            writeLock.tryLock();
            return proxyClientListeners.add(pushToClientListener);
        } finally {
            writeLock.unlock();
        }
    }

    public boolean removePushToClientSession(PushToClientSession pushToClientListener) {
        Lock writeLock = readWriteLock.writeLock();
        try {
            writeLock.tryLock();
            return proxyClientListeners.remove(pushToClientListener);
        } finally {
            writeLock.unlock();
        }
    }

    public void sendText(String message) {
        Lock readLock = readWriteLock.readLock();
        try {
            readLock.tryLock();
            int dex = selectStrategy.select(proxyClientListeners.size());
            if (dex >= 0) {
                PushToClientSession pushToClientListener = proxyClientListeners.get(dex);
                if (pushToClientListener == null) {
                    //如果因为程序出错,做一次补偿,取第一个
                    pushToClientListener = proxyClientListeners.get(0);
                }
                if (pushToClientListener != null) {
                    webSocketClientListener.handlePreSendingMsg(message);
                    pushToClientListener.sendMessage(message);
                }else {
                    webSocketClientListener.onSelectSessionError(message);
                }
            } else {
                Constants.wslogger.warn("Not find wsClient,message = [" + message + "] dropped");
            }
        } catch (Exception e) {
            Constants.wslogger.error("send text error:" + e.getMessage(), e);
        } finally {
            readLock.unlock();
        }
    }

}
