package ws.session.j2ee;

import ws.session.WsSessionAPI;

import javax.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public abstract class J2EEWsSession implements WsSessionAPI {

    protected Session session;

    protected boolean sync = false;

    public J2EEWsSession(Session session) {
        this.session = session;
    }

    @Override
    public void sendPing(byte[] ping) throws IOException {
        if (sync) {
            session.getBasicRemote().sendPing(ByteBuffer.wrap(ping));
        } else {
            session.getAsyncRemote().sendPing(ByteBuffer.wrap(ping));
        }
    }

    @Override
    public void sendPong(byte[] ping) throws IOException {
        if (sync) {
            session.getBasicRemote().sendPong(ByteBuffer.wrap(ping));
        } else {
            session.getAsyncRemote().sendPong(ByteBuffer.wrap(ping));
        }
    }

    @Override
    public void sendBinary(byte[] data) throws IOException {
        if (sync) {
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(data));
        } else {
            session.getAsyncRemote().sendBinary(ByteBuffer.wrap(data));
        }
    }

    @Override
    public void sendText(String txt) throws IOException {
        if (sync) {
            session.getBasicRemote().sendText(txt);
        } else {
            session.getAsyncRemote().sendText(txt);
        }
    }

    public void close() throws IOException {
        session.close();
    }

    public boolean isOpen() {
        return session.isOpen();
    }


    public void sendBinary(byte[] data,boolean first, boolean isLast) throws IOException {
        session.getBasicRemote().sendBinary(ByteBuffer.wrap(data), isLast);
    }


    public String id() {
        return session.getId();
    }

//    void close(CloseReason closeReason) throws IOException;
}
