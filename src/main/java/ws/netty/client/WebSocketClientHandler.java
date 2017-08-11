package ws.netty.client;

import ws.WsConstants;
import ws.client.WsProxyClient;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

/**
 * The webSocket Client handler
 * Created by ruiyong.hry on 10/08/2017.
 */
public class WebSocketClientHandler extends SimpleChannelInboundHandler<Object> {

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private WsProxyClient wsProxyClient;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker, WsProxyClient wsProxyClient) {
        this.handshaker = handshaker;
        this.wsProxyClient = wsProxyClient;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {

    }

    /**
     * 数据处理完成后,刷新出去
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            try {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                //session打开
                handshakeFuture.setSuccess();

            } catch (WebSocketHandshakeException e) {
                WsConstants.wslogger.error("WebSocket Client failed to connect" + e.getMessage(), e);
                handshakeFuture.setFailure(e);
            }
            return;
        }
        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.status() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }
        //handleSessionMessage
        try {
            WebSocketFrame frame = (WebSocketFrame) msg;
            if (frame instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;

            } else if (frame instanceof BinaryWebSocketFrame || frame instanceof ContinuationWebSocketFrame) {
                if (frame.isFinalFragment()) {
                    wsProxyClient.getSessionHandler().processFragment(readableBytes(frame.retain().content()), true);
                } else {
                    wsProxyClient.getSessionHandler().processFragment(readableBytes(frame.retain().content()), false);
                }
            } else if (frame instanceof PongWebSocketFrame) {
//            System.out.println("WebSocket Client received pong");
                //ignore
                return;
            } else if (frame instanceof CloseWebSocketFrame) {
                handshaker.close(ch, (CloseWebSocketFrame) frame.retain());
                wsProxyClient.getSessionHandler().onClose();
                return;
            }
        } catch (Exception e) {
            WsConstants.wslogger.error(e.getMessage(), e);
        }

    }

    private byte[] readableBytes(ByteBuf byteBuf) {
        if (!byteBuf.hasArray()) {
            int len = byteBuf.readableBytes();
            byte[] arr = new byte[len];
            byteBuf.getBytes(0, arr);
            return arr;
        }
        return new byte[0];
    }

    //onError
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        cause.printStackTrace();
        WsConstants.wslogger.error("WebSocketClientHandler.err" + cause.getMessage(), cause);
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        wsProxyClient.getSessionHandler().onError(wsProxyClient.getWsClientSession(), cause);
//        ctx.close();
    }
}
