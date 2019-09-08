package et.rpc.demo.hello.client;

import et.rpc.client.EtRpcClient;
import et.rpc.registry.ServiceDiscovery;
import et.rpc.registry.ZkClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EtRpcAutoConfiguration {

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
    public EtRpcClient etRpcClient(ZkClientFactory zkClientFactory){
        return new EtRpcClient(new ServiceDiscovery(zkClientFactory.getZkClient()));
    }
}
