package et.rpc.server.spring;

import et.rpc.registry.ServiceRegistry;
import et.rpc.registry.ZkClientFactory;
import et.rpc.server.EtRpcServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.collections4.MapUtils.isNotEmpty;

/**
 * @author wangl
 */
@Slf4j
public class SpringEtRpcServer implements ApplicationContextAware, InitializingBean, Ordered {

    /**
     * 服务端口
     */
    private int port;

    /**
     * 注册地址 zk 地址
     */
    private ZkClientFactory zkClientFactory;

    public SpringEtRpcServer(int port, ZkClientFactory zkClientFactory) {
        this.port = port;
        this.zkClientFactory = zkClientFactory;
    }

    private Map<String, Object> serviceMap = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        Map<String, Object> serverBeanMap = ctx.getBeansWithAnnotation(EtService.class);
        if(isNotEmpty(serverBeanMap)){
            serverBeanMap.forEach( (k, bean) -> {
                EtService etService = bean.getClass().getAnnotation(EtService.class);
                String serviceName = etService.value().getName();
                serviceMap.put(serviceName, bean);
            });
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ServiceRegistry serviceRegistry = new ServiceRegistry(zkClientFactory.getZkClient());
        EtRpcServer etRpcServer = new EtRpcServer(port, serviceRegistry, serviceMap);
        etRpcServer.started();
    }
}
