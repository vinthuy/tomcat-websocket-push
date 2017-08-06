package start;


import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import ws.*;
import ws.client.WsProxyClient;
import ws.client.WsPullHttp;
import ws.protocol.ext.WsTsPortClientHandleImpl;
import ws.server.MsgForwarder;
import ws.server.PushServer;
import ws.server.ext.DistributedCacheListener;
import ws.util.EnvUtil;
import wshandle.WsConfig;

import javax.annotation.Resource;
import java.util.List;

/**
 * 框架启动类,按需启动设置参数
 * Created by ruiyong.hry on 04/07/2017.
 */
public class EnvStart extends WsPullHttp implements WsContainer.WsConfigFace<EnvConfig> {

    private String serverAddr = "/ws/pushClient.ws?p=";


    @Setter
    @Getter
    private int currentEnv;

    @Resource
    private DistributedCacheListener distributedCacheListener;

    private String usWsOnlineUrl;

    private String wsOnlineUrl;

    @Resource
    private MsgForwarder msgForwarder;

    private WsContainer wsContainer;

    public void init() throws Exception {
        wsContainer = WsContainerSingle.instance();
        EnvUtil.curEnv = currentEnv;

        String config = readEnvConfig();

        final EnvConfig envConfig = JSON.parseObject(config, EnvConfig.class);
        envConfig.setEnv(EnvUtil.curEnv);
        if (envConfig != null) {

            wsContainer.triggerStart(this, envConfig);
            registerWsResultHandle();
        }

    }

    //启动读取配置
    private String readEnvConfig() {
        return null;
    }

    private void registerWsResultHandle() {
        WsTsPortClientHandleImpl wsTsPortHandle = (WsTsPortClientHandleImpl) wsContainer.getClientWsTsPortHandle();
        wsTsPortHandle.registerWsResultHandler(PushMsgType.OK, new OkWsResultHandlerImpl());
        wsContainer.setWsPull(this);
    }


    /**
     * 初始化客户端连接
     *
     * @param envConfig
     * @return
     */
    public boolean initWsClient(EnvConfig envConfig) {
        boolean result = false;
        wsContainer.newWsProxyClient(wsOnlineUrl);
        return result;
    }

    private String getWsUrl(String zoneHttpUrl, WsConfig.PushServerKey pushServerKey) {
        return zoneHttpUrl.replace("http://", "ws://") + serverAddr + pushServerKey.getKey();
    }

    @Override
    public boolean postWsServer(EnvConfig wsConfigDO) {
        PushServer pushServer = wsContainer.getPushServer();
        pushServer.setServerSessionListener(distributedCacheListener);
        pushServer.setSessionManager(distributedCacheListener);
        pushServer.setMsgForwarder(msgForwarder);
        WsConfig.PushServerKey[] pushServerKeys = WsConfig.PushServerKey.values();
        for (WsConfig.PushServerKey pushServerKey : pushServerKeys) {
            pushServer.getSessionManager().clearSessionCount(pushServerKey.getKey());
        }
        return true;
    }

    @Override
    public void checkWsClient(EnvConfig envConfig, List<WsProxyClient> list) {
        if (CollectionUtils.isEmpty(list)) {
            initWsClient(envConfig);
            return;
        }
        //检查客户端动作
    }


    @Override
    public String getPullServerUrl(String key) {
        return null;
    }
}
