package et.rpc.client;

import et.rpc.common.bean.EtRpcRequest;
import et.rpc.common.bean.EtRpcResponse;
import et.rpc.common.codec.EtRpcDecoder;
import et.rpc.common.codec.EtRpcEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@RequiredArgsConstructor
class NettyClient {
    private Map<String, Response> responseMap = new ConcurrentHashMap<>();
    private final EtRpcClientHandler handler =  new EtRpcClientHandler(responseMap);

    private final String host;
    private final int port;
    private Channel channel;
    private Bootstrap bootstrap;
    private EventLoopGroup group;

    public void doOpen() throws InterruptedException {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new EtRpcEncoder(EtRpcRequest.class));
                pipeline.addLast(new EtRpcDecoder(EtRpcResponse.class));
                pipeline.addLast(handler);
            }
        });

    }

    public void doConnect() throws InterruptedException {
        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();
    }

    public  void doClose(){
        try {
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            group.shutdownGracefully();
        }
    }
    private Channel getChannel() throws InterruptedException {
        Channel c = channel;
        if (c == null || !c.isActive()){
            doConnect();
        }
        return channel;
    }

    public EtRpcResponse send(EtRpcRequest request) throws InterruptedException {
        Channel c = getChannel();
        try {
            Response response =  new Response(request.getRequestId());
            responseMap.put(request.getRequestId(), response);
            c.writeAndFlush(request);
            return response.get();
        }finally {
            responseMap.remove(request.getRequestId());
        }
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    public static class NettyClientKey{
        private final String host;
        private final  int port;
    }
}
