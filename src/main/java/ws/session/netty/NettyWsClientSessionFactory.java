package ws.session.netty;

import ws.client.WsProxyClient;
import ws.netty.client.WebSocketClientHandler;
import ws.session.WsClientSessionFactory;
import ws.util.HostServerUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;

/**
 * Created by ruiyong.hry on 10/08/2017.
 */
public class NettyWsClientSessionFactory implements WsClientSessionFactory<NettyWsClientSession> {

    private final String WS = "ws";
    private final String WSS = "wss";

    //netty线程池
    private EventLoopGroup group = new NioEventLoopGroup();

    @Override
    public NettyWsClientSession newSession(WsProxyClient wsProxyClient) throws Exception {
        URI uri = wsProxyClient.getUri();
        String scheme = uri.getScheme() == null ? WS : uri.getScheme();
        final String host = uri.getHost() == null ? HostServerUtil.getLocalIp() : uri.getHost();
        final int port;
        if (uri.getPort() == -1) {
            if (WS.equalsIgnoreCase(scheme)) {
                port = 80;
            } else if (WSS.equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!WS.equalsIgnoreCase(scheme) && !WSS.equalsIgnoreCase(scheme)) {
            throw new IllegalStateException("Only WS(S) is supported.");
        }

        final boolean ssl = WSS.equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }
        //连接
        final WebSocketClientHandler handler = new WebSocketClientHandler(
                WebSocketClientHandshakerFactory.newHandshaker(
                        uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()), wsProxyClient
        );
        Bootstrap bootstrap = new Bootstrap();
        //超时时间3分钟
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, wsProxyClient.getWsContainer().getWsConfigDO().getSessionTimeOut());
        bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                if (sslCtx != null) {
                    pipeline.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                }
                pipeline.addLast(new HttpClientCodec(), new HttpObjectAggregator(65536), handler);
            }
        });

        Channel ch = bootstrap.connect(uri.getHost(), port).sync().channel();

//        handler.handshakeFuture().syncUninterruptibly();
        handler.handshakeFuture().awaitUninterruptibly();
        //连接成功
        return new NettyWsClientSession(ch);
    }

    @Override
    public void destory() {
        group.shutdownGracefully();
    }

    @Override
    public NettyWsClientSessioner newWsClientSessionListener(WsProxyClient wsProxyClient) {
        return new NettyWsClientSessioner(wsProxyClient);
    }
}
