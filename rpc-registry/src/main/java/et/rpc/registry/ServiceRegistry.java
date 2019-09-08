package et.rpc.registry;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;

/**
 * @author wangl
 */
@Slf4j
public class ServiceRegistry {

    private ZkClient zkClient;

    public ServiceRegistry(ZkClient zkClient) {
        this.zkClient = zkClient;
    }

    public void register(String serviceName, String serviceAddress) {
        if(!zkClient.exists(Constant.ZK_ROOT_PATH)){
            zkClient.createPersistent(Constant.ZK_ROOT_PATH);
            log.debug("create registry node: {}", Constant.ZK_ROOT_PATH);
        }
        // 创建 registry 节点
        String registryPath = Constant.ZK_REGISTRY_PATH;
        if(!zkClient.exists(registryPath)){
            zkClient.createPersistent(registryPath);
            log.debug("create registry node: {}", registryPath);
        }
        // 创建 service 节点
        String servicePath = registryPath + "/" + serviceName;
        if(!zkClient.exists(servicePath)){
            zkClient.createPersistent(servicePath);
            log.debug("create service node: {}", servicePath);
        }
        // 创建 address 节点
        String addressPath = servicePath + "/address-";
        String addressNode = zkClient.createEphemeralSequential(addressPath, serviceAddress);
        log.debug("create address node: {}", addressNode);
    }
}

