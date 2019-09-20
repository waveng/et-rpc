package et.rpc.spring;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

public class EtReferenceRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        registry(registry, ZkClientFactoryRegistrar.class);
        registry(registry, SpringEtReference.class);
        registry(registry, SpringEtRpcServer.class);
    }

    private  void registry(BeanDefinitionRegistry registry, Class clas){
        BeanDefinitionBuilder builder = rootBeanDefinition(clas);
        registry.registerBeanDefinition(clas.getName(), builder.getBeanDefinition());
    }
}
