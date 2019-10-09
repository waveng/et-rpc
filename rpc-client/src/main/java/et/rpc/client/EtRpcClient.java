package et.rpc.client;

import et.rpc.common.bean.EtRpcRequest;
import et.rpc.common.bean.EtRpcResponse;
import et.rpc.common.codec.EtRpcDecoder;
import et.rpc.common.codec.EtRpcEncoder;
import et.rpc.registry.ServiceDiscovery;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class EtRpcClient {
    @NonNull
    private ServiceDiscovery serviceDiscovery;
    private Map<NettyClient.NettyClientKey, NettyClient> nettyClientMap = new ConcurrentHashMap<>();

    private Map<Class<?>, Object> proxyCache = new HashMap<>();

    public synchronized  <T> T create(final Class<?> interfaceClass){
        if(proxyCache.containsKey(interfaceClass)){
            return (T) proxyCache.get(interfaceClass);
        }
        Object proxyInstance = Proxy.newProxyInstance(interfaceClass.getClassLoader(),new Class[]{interfaceClass},
             (proxy, method, args) ->{

                 String serviceName = interfaceClass.getName();
                 String serviceAddress = serviceDiscovery.discover(serviceName);
                 log.debug("discover service : {} => {}", serviceName, serviceAddress);

                 if(StringUtils.isEmpty(serviceAddress)){
                     throw new RuntimeException("server addess is empty!");
                 }
                 String[] array = StringUtils.split(serviceAddress, ":");
                 String host = array[0];
                 int port = Integer.parseInt(array[1]);

                 EtRpcRequest request =
                         EtRpcRequest.builder().requestId(UUID.randomUUID().toString())
                                 .interfaceName(method.getDeclaringClass().getName())
                                 .methodName(method.getName())
                                 .parameterTypes(method.getParameterTypes())
                                 .parameters(args).build();

                 EtRpcResponse response = send(request, host, port);
                 if(response == null){
                    log.error("send request failure", new IllegalStateException("response is null"));
                     return  null;
                 }

                 if(response.hasException()){
                     log.error("response has exception ", response.getException());
                     return  null;
                 }

                 return response.getResult();

             });
        proxyCache.put(interfaceClass, proxyInstance);
        return  (T) proxyInstance;
    }

    private EtRpcResponse send(EtRpcRequest request, String host, int port) throws InterruptedException {
        NettyClient.NettyClientKey nettyClientKey = new NettyClient.NettyClientKey(host, port);
        NettyClient nettyClient = nettyClientMap.get(nettyClientKey);
        if(nettyClient == null){
            synchronized (nettyClientMap) {
                nettyClient = nettyClientMap.get(nettyClientKey);
                if(nettyClient == null) {
                    nettyClient = new NettyClient(host, port);
                    nettyClient.doOpen();
                    nettyClientMap.put(nettyClientKey, nettyClient);
                }
            }
        }
        return nettyClient.send(request);

    }



}
