package ws.session.j2ee;

import ws.WsConstants;
import ws.session.WsClientSession;
import org.apache.commons.lang3.ObjectUtils;

import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public class J2EEWsClientSession extends J2EEWsSession implements WsClientSession {

    public J2EEWsClientSession(Session session) {
        super(session);
    }

    @Override
    public boolean heart() {
//        ByteBuffer buffer = null;
        try {
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(WsConstants.requestHeartBytes));
        } catch (Exception e) {
            WsConstants.wslogger.error("wsClient send ping error " + e.getMessage());
            if (e instanceof IOException) {
                try {
                    close();
                } catch (Exception ex) {
                    //ºöÂÔ
                    return false;
                }
            }

        } finally {
//            if (buffer != null) {
//
//                WsContainer.heartBufferPool.release(buffer);
//            }
        }
        return true;
    }

    public boolean isSameId(WsClientSession other) {
        return ObjectUtils.equals(other.id(), this.id());
    }


}
