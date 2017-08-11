package ws.session.netty;

import ws.WsConstants;
import ws.session.WsClientSession;
import io.netty.channel.Channel;
import org.apache.commons.lang3.ObjectUtils;

import java.io.IOException;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public class NettyWsClientSession extends NettySession implements WsClientSession {

    public NettyWsClientSession(Channel channel) {
        super(channel);
    }

    @Override
    public boolean heart() {
        try {
//            sendBinary(WsConstants.requestHeartBytes);
            sendPing(WsConstants.requestHeartBytes); //todo 心跳需要完善
        } catch (Exception e) {
            WsConstants.wslogger.error("wsClient send ping error " + e.getMessage());
            if (e instanceof IOException) {
                try {
                    close();
                } catch (Exception ex) {
                    //忽略
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isSameId(WsClientSession other) {
        return ObjectUtils.equals(other.id(), this.id());
    }
}
