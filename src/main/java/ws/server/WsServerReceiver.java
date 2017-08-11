package ws.server;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 内部接口路由信息进行处理
 * Created by ruiyong.hry on 09/08/2017.
 */
public interface WsServerReceiver {
    public void receive(InputStream requestInstream, OutputStream outputStream) throws Exception;
}
