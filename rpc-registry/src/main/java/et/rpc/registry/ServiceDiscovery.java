package et.rpc.registry;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
public class ServiceDiscovery {
    @NonNull
    private ZkClient zkClient;

    public String discover(String name){
        String servicePath = Constant.ZK_REGISTRY_PATH + "/" + name;
        if(!zkClient.exists(servicePath)){
            throw  new  RuntimeException(String.format("can not find any service node on path: %s", servicePath));
        }
        List<String> addressList = zkClient.getChildren(servicePath);
        if(CollectionUtils.isEmpty(addressList)){
            throw new RuntimeException(String.format("can not find any address node on path: %s", servicePath));
        }
        String address = null;
        int size = addressList.size();
        if(size == 1){
            address = addressList.get(0);
            log.debug("get only address node : {}", address);
        }else{
            address = addressList.get(ThreadLocalRandom.current().nextInt(size));
            log.debug("get random address node: {}", address);
        }
        String addressPath = servicePath + "/" + address;
        return  zkClient.readData(addressPath);
    }

}
