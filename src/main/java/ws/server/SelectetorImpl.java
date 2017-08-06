package ws.server;

import org.apache.commons.collections.MapUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Created by ruiyong.hry on 05/08/2017.
 */
public class SelectetorImpl implements Selectetor {

    private volatile String lastSessionId;

    @Override
    public PushToClientSession select(ConcurrentSkipListMap<String, PushToClientSession> skipListMap) {
        if (MapUtils.isEmpty(skipListMap)) {
            return null;
        }

        String sessionId = lastSessionId;
        if (sessionId == null) {
            Map.Entry<String, PushToClientSession> firstEntry = skipListMap.firstEntry();
            if(firstEntry==null){
                return null;
            }
            lastSessionId = firstEntry.getKey();
            return firstEntry.getValue();
        }

        PushToClientSession pushToClientSession = null;
        while (pushToClientSession == null) {
            String higherKey = skipListMap.higherKey(sessionId);

            if (higherKey == null) {
                //重头开始
                Map.Entry<String, PushToClientSession> firstEntry = skipListMap.firstEntry();
                if(firstEntry==null){
                    return null;
                }
                lastSessionId = firstEntry.getKey();
                return firstEntry.getValue();
            }

            lastSessionId = higherKey;
            pushToClientSession = skipListMap.get(higherKey);
            if (pushToClientSession != null) {
                return pushToClientSession;
            }
            //如果为空就移除
            skipListMap.remove(higherKey);
        }
        return null;
    }
}
