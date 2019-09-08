package et.rpc.server;

import et.rpc.common.bean.EtRpcRequest;
import et.rpc.common.bean.EtRpcResponse;
import et.rpc.common.codec.EtRpcDecoder;
import et.rpc.common.codec.EtRpcEncoder;
import et.rpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Map;

/**
 * @author wangl
 */
@Slf4j
public class EtRpcServer {

    private int port;

    private ServiceRegistry serviceRegistry;
    
    private Map<String, Object> serviceMap;

    public EtRpcServer(int port, ServiceRegistry serviceRegistry, Map<String, Object> serviceMap) {
        this.port = port;
        this.serviceRegistry = serviceRegistry;
        this.serviceMap = serviceMap;
    }

    public void started(){
        EventLoopGroup group = new NioEventLoopGroup();
        EventLoopGroup childGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = bootstrap(group, childGroup);
            ChannelFuture future = bind(bootstrap);
            registry();
            future.channel().closeFuture().sync();
        }catch (Exception e){
            log.error("server exdeption", e);
        }finally {
            childGroup.shutdownGracefully();
            group.shutdownGracefully();
        }
    }

    private ChannelFuture bind(ServerBootstrap bootstrap) throws InterruptedException {

        ChannelFuture future = bootstrap.bind(port).sync();
        log.debug("server started, listening on:{}", port);
        return future;
    }

    private ServerBootstrap bootstrap(EventLoopGroup group, EventLoopGroup childGroup) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(group, childGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new EtRpcDecoder(EtRpcRequest.class));
                pipeline.addLast(new EtRpcEncoder(EtRpcResponse.class));
                pipeline.addLast(new EtRpcServerHandler(serviceMap));
            }
        });
        return bootstrap;
    }

    private void registry(){
        String serviceAddress = InetAddress.getLoopbackAddress().getHostAddress() + ":" + port;
        for (String interfaceName : serviceMap.keySet()) {
            serviceRegistry.register(interfaceName, serviceAddress);
            log.debug("registry service: {} => {}", interfaceName, serviceAddress);
        }
    }
}
