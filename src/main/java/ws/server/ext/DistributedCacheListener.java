package ws.server.ext;

import com.google.common.collect.Lists;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import ws.Constants;
import ws.DistributeCacheService;
import ws.model.EnvCacheValue;
import ws.model.ServerEndpoint;
import ws.server.PushToClientSession;
import ws.server.ServerSessionListener;
import ws.server.SessionManager;
import ws.util.EnvUtil;
import ws.util.HostServerUtil;

import javax.websocket.Session;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by ruiyong.hry on 14/07/2017.
 */
public class DistributedCacheListener implements ServerSessionListener, SessionManager {

    private final static String cache_prefix = "dm-ws-";


    private DistributeCacheService tairCommonService;

    private int expirdTime = 1800;

    @Override
    public void onStart(PushToClientSession session) {
        String key = session.getKey();
        Session ss = session.getSession();
        if (ss != null && ss.isOpen()) {
            incrSessionCount(key, ss.getId());
        }
    }

    @Override
    public void onClose(PushToClientSession session) {
        String key = session.getKey();
        Session ss = session.getSession();
        if (ss != null) {
            decrSessionCount(key, ss.getId());
        }

    }

    @Override
    public void onError(PushToClientSession session) {

    }


    private String getCaCheEnvKey(String key) {
        return cache_prefix + key;
    }

    private String genCaCheEnvValue(String ip, int port, int serverEnv) {
        return new StringBuilder(ip).append(":").append(port).append(":").append(serverEnv).toString();
    }


    private String genLocalCaCheEnvValue() {
        String curServerIp = HostServerUtil.getLocalIp();
        int port = HostServerUtil.getPort();
        return genCaCheEnvValue(curServerIp, port, EnvUtil.getEnv());
    }


//    @Override
//    public List<EnvCacheValue> getCurrentServerIpSession(String key) {
//        List<EnvCacheValue> sessions = getAllSession(key);
//        if (CollectionUtils.isNotEmpty(sessions)) {
//            List<EnvCacheValue> result = Lists.newArrayList();
//            for (EnvCacheValue envCacheValue : sessions) {
//                ServerEndpoint serverEndpoint = envCacheValue.getServerEndpoint();
//                if (serverEndpoint != null) {
//
//                    if (ObjectUtils.equals(serverEndpoint.getPort(), HostServerUtil.getPort()) &&
//                            ObjectUtils.equals(serverEndpoint.getServerIp(), HostServerUtil.getLocalIp())) {
//                        result.add(envCacheValue);
//                    }
//                }
//            }
//            return result;
//        }
//        return null;
//    }
//
//    public List<EnvCacheValue> getAllSession(String key) {
//        try {
//            Map<String, Serializable> sessionMap = tairCommonService.getGroup(getCaCheEnvKey(key));
//            if (MapUtils.isNotEmpty(sessionMap)) {
//                List<EnvCacheValue> list = Lists.newArrayList();
//                for (String sessionIp : sessionMap.keySet()) {
//                    String[] sessionIpArray = sessionIp.split(":");
//                    if (sessionIpArray != null && sessionIpArray.length > 2) {
//                        EnvCacheValue envCacheValue = new EnvCacheValue();
//                        String ip = sessionIpArray[0];
//                        String port = sessionIpArray[1];
//                        String sessionId = sessionIpArray[2];
//                        ServerEndpoint serverEndpoint = new ServerEndpoint(ip, Integer.parseInt(port));
//                        envCacheValue.setServerEndpoint(serverEndpoint);
//                        envCacheValue.setSessionId(sessionId);
//                        Serializable val = sessionMap.get(sessionIp);
//                        envCacheValue.setServerEnv(Integer.parseInt(val.toString()));
//                        list.add(envCacheValue);
//                    }
//                }
//                return list;
//            }
//        } catch (Exception e) {
//            Constants.wslogger.error("PushToClientSession cache session error:" + e.getMessage(), e);
//        }
//        return null;
//    }

    @Override
    public EnvCacheValue getCurrentServerIpSessionCount(String key) {
        String curServerIp = HostServerUtil.getLocalIp();
        int port = HostServerUtil.getPort();
        Serializable value = null;
        try {
            value = tairCommonService.gutGroupObj(getCaCheEnvKey(key), genCaCheEnvValue(curServerIp, port, EnvUtil.getEnv()));
        } catch (Exception e) {
            Constants.wslogger.error("getCurrentServerIpSessionCount error:" + e.getMessage(), e);
        }
        EnvCacheValue envCacheValue = new EnvCacheValue();
        if (value == null) {
            envCacheValue.setSessionCount(0);
        } else {
            envCacheValue.setSessionCount((Integer) value);
        }
        envCacheValue.setServerEnv(EnvUtil.getEnv());
        envCacheValue.setServerEndpoint(new ServerEndpoint(curServerIp, port));
        return envCacheValue;
    }

    @Override
    public List<EnvCacheValue> getAllSessionCount(String key) {
        try {
            Map<String, Serializable> sessionMap = tairCommonService.getGroup(getCaCheEnvKey(key));
            if (MapUtils.isNotEmpty(sessionMap)) {
                List<EnvCacheValue> list = Lists.newArrayList();
                for (String sessionIp : sessionMap.keySet()) {
                    String[] sessionIpArray = sessionIp.split(":");
                    if (sessionIpArray != null && sessionIpArray.length > 2) {
                        EnvCacheValue envCacheValue = new EnvCacheValue();
                        String ip = sessionIpArray[0];
                        String port = sessionIpArray[1];
                        String serverEnv = sessionIpArray[2];
                        ServerEndpoint serverEndpoint = new ServerEndpoint(ip, Integer.parseInt(port));
                        if (StringUtils.isNotBlank(serverEnv)) {
                            envCacheValue.setServerEnv(Integer.parseInt(serverEnv));
                        }
                        envCacheValue.setServerEndpoint(serverEndpoint);
                        Serializable count = sessionMap.get(sessionIp);
                        if (count == null) {
                            envCacheValue.setSessionCount(0);
                        } else {
                            envCacheValue.setSessionCount((Integer) count);
                        }
                        list.add(envCacheValue);
                    }
                }
                return list;
            }
        } catch (Exception e) {

        }
        return null;
    }

    @Override
    public void incrSessionCount(String key, String sessionId) {
        try {
            //默认从0,为空时值为1
            tairCommonService.prefixIncr(getCaCheEnvKey(key), genLocalCaCheEnvValue(), 1, 0, expirdTime);
        } catch (Exception e) {
            Constants.wslogger.error("incr SessionCount error:" + e.getMessage(), e);
        }
    }

    @Override
    public void decrSessionCount(String key, String sessionId) {
        try {
            //默认从1,为空时值为0
            tairCommonService.prefixDecr(getCaCheEnvKey(key), genLocalCaCheEnvValue(), 1, 1, expirdTime);
        } catch (Exception e) {
            Constants.wslogger.error("decr SessionCount error:" + e.getMessage(), e);
        }
    }

    @Override
    public void clearSessionCount(String key) {
        try {
            tairCommonService.prefixDelete(getCaCheEnvKey(key), genLocalCaCheEnvValue());
        } catch (Exception e) {
            Constants.wslogger.error("clearSessionCount error:" + e.getMessage(), e);
        }
    }

    @Override
    public void setSessionCount(String key, int count) {
        try {
            tairCommonService.prefixSetCount(getCaCheEnvKey(key), genLocalCaCheEnvValue(), count, expirdTime);
        } catch (Exception e) {
            Constants.wslogger.error("setSessionCount error:" + e.getMessage(), e);
        }
    }


//    @Override
//    public void removeSession(String key, String sessionId) {
//        String curServerIp = HostServerUtil.getLocalIp();
//        int port = HostServerUtil.getPort();
//        try {
//            tairCommonService.invalidGroup(getCaCheEnvKey(key), genCaCheEnvValue(curServerIp, port, sessionId));
//        } catch (Exception e) {
//            Constants.wslogger.error("PushToClientSession onClose session cache error:" + e.getMessage(), e);
//        }
//    }


}
