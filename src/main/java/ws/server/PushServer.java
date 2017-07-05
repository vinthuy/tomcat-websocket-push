package ws.server;


import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import ws.Constants;
import ws.model.WsResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private Map<String, List<PushToClientSession>> proxyClientListenersMap;

    private ReadWriteLock readWriteLock;


    public PushServer() {
        readWriteLock = new ReentrantReadWriteLock();
        proxyClientListenersMap = new ConcurrentHashMap<String, List<PushToClientSession>>();
        selectStrategy = new SelectStrategyImpl();
        webSocketClientListener = new EmptyWebSocketClientListener();
    }

    public boolean addPushToClientSession(String key, PushToClientSession pushToClientListener) {
        List<PushToClientSession> list = proxyClientListenersMap.get(key);

        Lock writeLock = readWriteLock.writeLock();
        if (writeLock.tryLock()) {
            try {
                if (list == null) {
                    list = new ArrayList<PushToClientSession>();
                    proxyClientListenersMap.put(key, list);
                }
                return list.add(pushToClientListener);
            } finally {
                writeLock.unlock();
            }
        }
        return false;
    }

    public boolean removePushToClientSession(String key, PushToClientSession pushToClientListener) {
        List<PushToClientSession> list = proxyClientListenersMap.get(key);
        Lock writeLock = readWriteLock.writeLock();
        if (writeLock.tryLock()) {
            try {
                if (list != null) {
                    return list.remove(pushToClientListener);
                }
            } finally {
                writeLock.unlock();
            }
        }

        return false;
    }

    public void sendText(String key, Object message, MsgParse msgParse) {
        List<PushToClientSession> list = proxyClientListenersMap.get(key);
        if (list == null) {
            Constants.wslogger.warn("Not find useful push-to-session");
            return;
        }
        Lock readLock = readWriteLock.readLock();
        if (readLock.tryLock()) {
            try {
                int dex = selectStrategy.select(list.size());
                if (dex >= 0) {
                    PushToClientSession pushToClientListener = list.get(dex);
                    if (pushToClientListener == null) {
                        //如果因为程序出错,做一次补偿,取第一个
                        pushToClientListener = list.get(0);
                    }
                    if (pushToClientListener != null) {
                        webSocketClientListener.handlePreSendingMsg(message);
                        String txt = msgParse.parse(message);
                        if (StringUtils.isNotBlank(txt)) {
                            pushToClientListener.sendMessage(txt);
                        }
                    } else {
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

    public interface MsgParse {
        public String parse(Object msg);
    }

    public void SendWsResultProtocol(String clienKey, final WsResult wsResult) {
        sendText(clienKey, wsResult, new MsgParse() {
            @Override
            public String parse(Object msg) {
                return JSONObject.toJSONString(msg);
            }
        });
    }

}
