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

/**
 * @author wangl
 */
public class SpringEtReference implements BeanPostProcessor, BeanFactoryPostProcessor, EnvironmentAware, Ordered {

    private  ConfigurableListableBeanFactory beanFactory;
    private  Environment environment;
    private  EtRpcClient etRpcClient;


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), (field) -> {
            EtReference etReference = null;
            if((etReference = field.getAnnotation(EtReference.class)) != null){

                Object b;
                if(beanFactory.containsBean(etReference.value().getName())) {
                    b = beanFactory.getBean(etReference.value());
                }else{
                    b = etRpcClient.create(etReference.value());
                    beanFactory.registerSingleton(etReference.value().getName(), b);
                }
                field.setAccessible(true);
                ReflectionUtils.setField(field, bean, b);
                field.setAccessible(false);
            }
        });
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        this.beanFactory.registerSingleton(EtRpcClient.class.getSimpleName(), etRpcClient(zkClientFactory()));
        this.etRpcClient  = this.beanFactory.getBean(EtRpcClient.class);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public ZkClientFactory zkClientFactory(){
        return beanFactory.getBean(ZkClientFactory.class);
    }

    public EtRpcClient etRpcClient(ZkClientFactory zkClientFactory){
        return new EtRpcClient(new ServiceDiscovery(zkClientFactory.getZkClient()));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
