import ws.OkWsResultHandlerImpl;
import ws.PushMsgType;
import ws.WsContainer;
import ws.WsContainerSingle;
import ws.client.WsProxyClient;
import ws.protocol.ext.WsTsPortClientHandleImpl;
import ws.server.PushServer;
import ws.session.WsClientSessionFactory;
import ws.session.netty.NettyWsClientSession;
import ws.session.netty.NettyWsClientSessionFactory;
import ws.util.HostServerUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public class WsEvnStartEx implements WsContainer.WsConfigFace<WsContainer.WsConfigDO> {

    private String serverAddr = "/ws/pushClient.ws?p=%s&ch=%s";


    private WsContainer wsContainer;

    private WsClientSessionFactory<NettyWsClientSession> wsClientSessionFactory = new NettyWsClientSessionFactory();

    public void afterPropertiesSet() throws Exception {
        wsContainer = WsContainerSingle.instance();
        //�����ͻ�������
        //1.������������
        WsContainer.WsConfigDO envConfig = new WsContainer.WsConfigDO();
        envConfig.setWsServerStart(false);
        //2.����
        wsContainer.triggerStartOnWsClientMode(this, envConfig);
        //ע��ҵ������
        registerWsResultHandle();

    }

    private void registerWsResultHandle() {
        WsTsPortClientHandleImpl wsTsPortHandle = (WsTsPortClientHandleImpl) wsContainer.getClientWsTsPortHandle();
        //ע��ҵ����ش�����
        wsTsPortHandle.registerWsResultHandler(PushMsgType.OK, new OkWsResultHandlerImpl());
    }


    public boolean initWsClient(WsContainer.WsConfigDO envConfig) {
        String wsUrl = getWsUrl();
        if (StringUtils.isNotBlank(wsUrl)) {
            wsContainer.newWsProxyClient(wsUrl, wsClientSessionFactory);
            return true;
        }
        return false;
    }


    private String getWsUrl() {
        return "ws://localhost:8081" + String.format(serverAddr, genWsSessionGroupKey(), HostServerUtil.getLocalIp());
    }

    /**
     * ����wsClient������key
     *
     * @return
     */
    public static String genWsSessionGroupKey() {
        return new StringBuilder("dc@").append(736).append("@").append(182).toString();
    }

    public static boolean isWsSessionKey(String groupKey) {
        return groupKey.startsWith("dc@");
    }


    @Override
    public void checkWsClient(WsContainer.WsConfigDO wsConfigDO, Collection<WsProxyClient> list) {
        if (CollectionUtils.isEmpty(list)) {
            initWsClient(wsConfigDO);
            return;
        }
    }


    @Override
    public void buildPushServer(PushServer pushServer) {

    }


}


