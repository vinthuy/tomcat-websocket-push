package ws.session.netty;

import ws.WsConstants;
import ws.session.WsSessionAPI;
import io.netty.buffer.Unpooled;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.*;

import java.io.IOException;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public class NettySession implements WsSessionAPI {

    protected Channel channel;

    public NettySession(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void sendPing(byte[] ping) throws IOException {
        WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(WsConstants.requestHeartBytes));
        writeIf(frame);
    }

    private void writeIf(WebSocketFrame frame) {
        if (channel.isWritable()) {
            channel.writeAndFlush(frame);
        }
    }

    @Override
    public void sendPong(byte[] ping) throws IOException {
        WebSocketFrame frame = new PongWebSocketFrame(Unpooled.wrappedBuffer(WsConstants.requestHeartBytes));
        writeIf(frame);
    }

    @Override
    public void sendBinary(byte[] data) throws IOException {
        WebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data));
        writeIf(frame);
    }

    @Override
    public void sendBinary(byte[] data, boolean first, boolean isLast) throws IOException {
        WebSocketFrame frame = null;
        if(first){
            frame  = new BinaryWebSocketFrame(isLast, 0, Unpooled.wrappedBuffer(data));
        }else {
            frame = new ContinuationWebSocketFrame(isLast, 0, Unpooled.wrappedBuffer(data));
        }
        writeIf(frame);
    }

    @Override
    public void sendText(String txt) {
        WebSocketFrame frame = new TextWebSocketFrame(txt);
        writeIf(frame);
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public boolean isOpen() {
        return channel.isActive() && channel.isOpen();
    }

    @Override
    public String id() {
        return ((AbstractChannel) channel).id().asShortText();
    }
}
