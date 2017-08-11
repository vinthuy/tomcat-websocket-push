package ws.server;

import ws.model.EnvCacheValue;
import ws.model.ServerEndpoint;

import java.util.List;

/**
 * ���ڼ�Ⱥ������·��
 * Created by ruiyong.hry on 07/08/2017.
 */
public interface ServerSelector {

    public ServerEndpoint router(String key, List<EnvCacheValue> envCacheValues);

}
