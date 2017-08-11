package ws.client;

import ws.WsContainerSingle;
import ws.WsException;
import ws.session.j2ee.J2EEWsClientSession;
import ws.session.j2ee.J2EEWsClientSessioner;

import javax.websocket.*;

/**
 * websocket����ͻ���
 * �������Է���˵�������Ϣ
 *
 * @author ruiyong.hry
 */
@ClientEndpoint
public class WsProxyClientEndpoint {

    private J2EEWsClientSessioner j2EEWsClientSessioner;

    private J2EEWsClientSession j2EEWsSession;


    @OnOpen
    public void onOpen(Session session) {
        j2EEWsClientSessioner = wsClientSessionListener(session.getId());
        j2EEWsSession = wsClientSession(session.getId());
        j2EEWsClientSessioner.onOpen(j2EEWsSession);
//        this.session = session;
    }

    @OnClose
    public void onClose() {
        if (j2EEWsClientSessioner != null) {
            j2EEWsClientSessioner.onClose();
        }
    }


    private WsProxyClient wsProxyClient(String sesssionId) {
        return WsContainerSingle.instance().getWsProxyClient(sesssionId);
    }

    private J2EEWsClientSessioner wsClientSessionListener(String sessionId) {
        WsProxyClient wsProxyClient = wsProxyClient(sessionId);
        if (wsProxyClient != null) {
            return (J2EEWsClientSessioner) wsProxyClient.getSessionHandler();
        }
        throw new WsException("Not find ws client session listener");
    }

    private J2EEWsClientSession wsClientSession(String sessionId) {
        WsProxyClient wsProxyClient = wsProxyClient(sessionId);
        if (wsProxyClient != null) {
            return (J2EEWsClientSession) wsProxyClient.getWsClientSession();
        }
        throw new WsException("Not find ws client session");
    }

    /**
     * �����Ϣ�Ƿ�������͵�.
     * ���ڿͻ������ǲɼ����̴߳���
     * <p>
     * //     * @param message
     */
    // ��������Ϣ�߼�ѡ��������ն���������
    @OnMessage
    public void processFragment(byte[] responseData, boolean isLast, Session session) {
        j2EEWsClientSessioner.processFragment(responseData, isLast);
    }


    @OnError
    public void onError(Session session, Throwable t) {
        if (j2EEWsClientSessioner != null) {
            j2EEWsClientSessioner.onError(j2EEWsSession, t);
        }
    }


}