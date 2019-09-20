package et.rpc.spring;

import et.rpc.client.EtRpcClient;
import et.rpc.registry.ServiceDiscovery;
import et.rpc.registry.ZkClientFactory;
import et.rpc.spring.annotation.EtReference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

public class ZkClientFactoryRegistrar implements BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    private ConfigurableListableBeanFactory beanFactory;
    private Environment environment;


    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        this.beanFactory.registerSingleton(ZkClientFactory.class.getName(), zkClientFactory());
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public ZkClientFactory zkClientFactory(){
        return new ZkClientFactory(environment.getProperty("et.rpc.registry"));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
