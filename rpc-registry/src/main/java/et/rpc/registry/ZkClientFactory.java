package et.rpc.registry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;

@Slf4j
@Getter
public class ZkClientFactory {
    private String zkAddress;
    private ZkClient zkClient;

    public ZkClientFactory(String zkAddress) {
        this.zkAddress = zkAddress;
        this.zkClient = createZkClient(zkAddress);
    }

    private ZkClient createZkClient(String zkAddress) {
        ZkClient zk = new ZkClient(zkAddress, Constant.ZK_SESSIOON_TIMEOUT, Constant.ZK_CONNECTION_TIOMEOUT);
        log.debug("connect to zookeeper");
        return zk;
    }



}
