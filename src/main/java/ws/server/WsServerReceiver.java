package ws.server;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * �ڲ��ӿ�·����Ϣ���д���
 * Created by ruiyong.hry on 09/08/2017.
 */
public interface WsServerReceiver {
    public void receive(InputStream requestInstream, OutputStream outputStream) throws Exception;
}
