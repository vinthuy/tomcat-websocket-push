package ws.server;

import ws.model.EnvCacheValue;

import java.util.List;

/**
 * the session manager
 * key:和环境相关的配置
 * Created by ruiyong.hry on 14/07/2017.
 */
public interface SessionManager {
    //
    public EnvCacheValue getCurrentServerIpSessionCount(String key);

    public List<EnvCacheValue> getAllSessionCount(String key);

    public void incrSessionCount(String key, String sessionId);

    public void decrSessionCount(String key, String sessionId);

    public void clearSessionCount(String key);

    public void setSessionCount(String key, int count);
}
