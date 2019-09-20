package et.rpc.spring;

import et.rpc.registry.ServiceRegistry;
import et.rpc.registry.ZkClientFactory;
import et.rpc.server.EtRpcServer;
import et.rpc.spring.annotation.EtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.collections4.MapUtils.isNotEmpty;

/**
 * @author wangl
 */
@Slf4j
public class SpringEtRpcServer implements ApplicationContextAware, EnvironmentAware, InitializingBean, Ordered {

    /**
     * 服务端口
     */
    private int port;

    /**
     * 注册地址 zk 地址
     */
    private ZkClientFactory zkClientFactory;

    private ApplicationContext ctx;

    private Map<String, Object> serviceMap = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
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
        this.zkClientFactory = this.ctx.getBean(ZkClientFactory.class);
        ServiceRegistry serviceRegistry = new ServiceRegistry(zkClientFactory.getZkClient());
        EtRpcServer etRpcServer = new EtRpcServer(port, serviceRegistry, serviceMap);
        etRpcServer.started();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.port = environment.getProperty("et.rpc.port", Integer.class, 6210);
    }
}
