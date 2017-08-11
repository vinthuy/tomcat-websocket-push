package ws.server.ext;

import com.google.common.collect.Lists;
import ws.model.EnvCacheValue;
import ws.model.ServerEndpoint;
import ws.server.ServerSelector;
import ws.util.HostServerUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * the ServerSelectorImpl.
 * Created by ruiyong.hry on 07/08/2017.
 */
public class ServerSelectorImpl implements ServerSelector {


    public ServerEndpoint router(String key, List<EnvCacheValue> envCacheValues) {
        if (CollectionUtils.isNotEmpty(envCacheValues)) {
//            Map<ServerEndpoint, Integer> serverSessionCountMap = new HashMap<ServerEndpoint, Integer>();
            List<EnvCacheValue> validCaches = Lists.newArrayList();
            for (EnvCacheValue envCacheValue : envCacheValues) {
                //不同环境排除
                if (envCacheValue.getServerEnv() != HostServerUtil.getCurrentServerEnv()) {
                    continue;
                }
                //自身ip排除
                ServerEndpoint serverEnpoint = envCacheValue.getServerEndpoint();
                if (ObjectUtils.equals(serverEnpoint.getServerIp(), HostServerUtil.getLocalIp()) &&
                        ObjectUtils.equals(serverEnpoint.getPort(), HostServerUtil.getPort())) {
                    continue;
                }
                validCaches.add(envCacheValue);
            }
            //采用轮询实现
            if (CollectionUtils.isNotEmpty(validCaches)) {
                Collections.sort(validCaches, new Comparator<EnvCacheValue>() {
                    @Override
                    public int compare(EnvCacheValue o1, EnvCacheValue o2) {
                        if (o1.getSessionCount() < o2.getSessionCount()) {
                            return 1;
                        } else if (o1.getSessionCount() > o2.getSessionCount()) {
                            return -1;
                        }
                        return 0;
                    }
                });
                int routerIndex = selectBySize(envCacheValues.size());
                return envCacheValues.get(routerIndex).getServerEndpoint();
            }
        }
        return null;
    }


    public volatile int lastSelect = 0;

    public int selectBySize(int size) {
        int canSelectIndex = size - 1;
        int res = -1;
        if (canSelectIndex < 0) {
            res = -1;
        } else if (canSelectIndex == 0) {
            res = 0;
        } else if (lastSelect < canSelectIndex) {
            //返回在加+1
            res = lastSelect;
            lastSelect++;
        }

        //lastSelect >= canSelectIndex-1;
        else if (lastSelect > canSelectIndex) {
            lastSelect = 0;
            res = lastSelect;
        } else {
            res = lastSelect;
            lastSelect = 0;
        }
        return res;
    }


}
