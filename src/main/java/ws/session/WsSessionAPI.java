package ws.session;

import java.io.IOException;

/**
 * The ws session API
 * Created by ruiyong.hry on 10/08/2017.
 */
public interface WsSessionAPI {

    void sendPing(byte[] ping) throws IOException;

    void sendPong(byte[] ping) throws IOException;

    void sendBinary(byte[] data) throws IOException;

    void sendBinary(byte[] data, boolean first, boolean isLast) throws IOException;

    void sendText(String txt) throws IOException;

    void close() throws IOException;

    boolean isOpen();

    public String id();
}
