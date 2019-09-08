package et.rpc.demo.hello.server;

import et.rpc.registry.ZkClientFactory;
import et.rpc.server.spring.SpringEtRpcServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EtRpcAutoConfiguration {
    /**
     * 服务端口
     */
    @Value("${et.rpc.port}")
    private int port;

    /**
     * 注册地址 zk 地址
     */
    @Value("${et.rpc.registry}")
    private String zkAddress;

    @Bean
    public ZkClientFactory zkClientFactory(){
        return new ZkClientFactory(zkAddress);
    }

    @Bean
    public SpringEtRpcServer springEtRpcServer(ZkClientFactory zkClientFactory){
        return new SpringEtRpcServer(port, zkClientFactory);
    }
}
