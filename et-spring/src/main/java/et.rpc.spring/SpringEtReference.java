package et.rpc.spring;

import et.rpc.client.EtRpcClient;
import et.rpc.registry.ServiceDiscovery;
import et.rpc.registry.ZkClientFactory;
import et.rpc.spring.annotation.EtReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.springframework.core.BridgeMethodResolver.findBridgedMethod;
import static org.springframework.core.BridgeMethodResolver.isVisibilityBridgeMethodPair;
import static org.springframework.core.annotation.AnnotationUtils.*;

/**
 * @author wangl
 */
@Slf4j
public class SpringEtReference extends InstantiationAwareBeanPostProcessorAdapter implements
        MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryPostProcessor, EnvironmentAware {

    private  ConfigurableListableBeanFactory beanFactory;
    private  Environment environment;
    private  EtRpcClient etRpcClient;

    private final ConcurrentMap<String, SpringEtReference.AnnotatedInjectionMetadata> injectionMetadataCache =
            new ConcurrentHashMap<String, SpringEtReference.AnnotatedInjectionMetadata>(16);

    private final ConcurrentMap<Object, Object> injectedObjectsCache = new ConcurrentHashMap<Object, Object>(16);

    @Override
    public PropertyValues postProcessPropertyValues(
            PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeanCreationException {
        InjectionMetadata metadata = findInjectionMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        return pvs;
    }


    public class AnnotatedFieldElement extends InjectionMetadata.InjectedElement {

        private final Field field;

        private final EtReference annotation;

        private volatile Object injectedBean;

        protected AnnotatedFieldElement(Field field, EtReference annotation) {
            super(field, null);
            this.field = field;
            this.annotation = annotation;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

            Class<?> injectedType = field.getType();

            injectedBean = getInjectedObject(annotation, bean, beanName, injectedType, this);

            ReflectionUtils.makeAccessible(field);

            field.set(bean, injectedBean);

        }

        public Field getField() {
            return field;
        }

        public EtReference getAnnotation() {
            return annotation;
        }

        public Object getInjectedBean() {
            return injectedBean;
        }

    }

    public class AnnotatedMethodElement extends InjectionMetadata.InjectedElement {

        private final Method method;

        private final EtReference annotation;

        private volatile Object injectedBean;

        protected AnnotatedMethodElement(Method method, PropertyDescriptor pd, EtReference annotation) {
            super(method, pd);
            this.method = method;
            this.annotation = annotation;
        }

        @Override
        protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {

            Class<?> injectedType = pd.getPropertyType();

            injectedBean = getInjectedObject(annotation, bean, beanName, injectedType, this);

            ReflectionUtils.makeAccessible(method);

            method.invoke(bean, injectedBean);

        }

        public Method getMethod() {
            return method;
        }

        public EtReference getAnnotation() {
            return annotation;
        }

        public Object getInjectedBean() {
            return injectedBean;
        }

        public PropertyDescriptor getPd() {
            return this.pd;
        }
    }


    protected Object getInjectedObject(EtReference annotation, Object bean, String beanName, Class<?> injectedType,
                                       InjectionMetadata.InjectedElement injectedElement) throws Exception {

        Object cacheKey = buildInjectedObjectCacheKey(annotation, bean, beanName, injectedType, injectedElement);

        Object injectedObject = injectedObjectsCache.get(cacheKey);

        if (injectedObject == null) {
            injectedObject = doGetInjectedBean(annotation, bean, beanName, injectedType, injectedElement);
            // Customized inject-object if necessary
            injectedObjectsCache.putIfAbsent(cacheKey, injectedObject);
        }

        return injectedObject;

    }

    private Object buildInjectedObjectCacheKey(EtReference annotation, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        return annotation.value();
    }

    private Object doGetInjectedBean(EtReference annotation, Object bean, String beanName, Class<?> injectedType, InjectionMetadata.InjectedElement injectedElement) {
        return etRpcClient.create(annotation.value());
    }

    private InjectionMetadata findInjectionMetadata(String beanName, Class<?> clazz, PropertyValues pvs) {
        // Fall back to class name as cache key, for backwards compatibility with custom callers.
        String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
        // Quick check on the concurrent map first, with minimal locking.
        SpringEtReference.AnnotatedInjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
        if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            synchronized (this.injectionMetadataCache) {
                metadata = this.injectionMetadataCache.get(cacheKey);
                if (InjectionMetadata.needsRefresh(metadata, clazz)) {
                    if (metadata != null) {
                        metadata.clear(pvs);
                    }
                    try {
                        metadata = buildAnnotatedMetadata(clazz);
                        this.injectionMetadataCache.put(cacheKey, metadata);
                    } catch (NoClassDefFoundError err) {
                        throw new IllegalStateException("Failed to introspect object class [" + clazz.getName() +
                                "] for annotation metadata: could not find class that it depends on", err);
                    }
                }
            }
        }
        return metadata;
    }

    private List<InjectionMetadata.InjectedElement> findFieldAnnotationMetadata(final Class<?> beanClass) {

        final List<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectionMetadata.InjectedElement>();

        ReflectionUtils.doWithFields(beanClass, new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

                EtReference annotation = getAnnotation(field, getAnnotationType());
                if (annotation != null) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        if (log.isWarnEnabled()) {
                            log.warn("@" + getAnnotationType().getName() + " is not supported on static fields: " + field);
                        }
                        return;
                    }

                    elements.add(new AnnotatedFieldElement(field, annotation));
                }

            }
        });

        return elements;

    }


    private List<InjectionMetadata.InjectedElement> findAnnotatedMethodMetadata(final Class<?> beanClass) {

        final List<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectionMetadata.InjectedElement>();

        ReflectionUtils.doWithMethods(beanClass, new ReflectionUtils.MethodCallback() {
            @Override
            public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {

                Method bridgedMethod = findBridgedMethod(method);

                if (!isVisibilityBridgeMethodPair(method, bridgedMethod)) {
                    return;
                }

                EtReference annotation = findAnnotation(bridgedMethod, getAnnotationType());

                if (annotation != null && method.equals(ClassUtils.getMostSpecificMethod(method, beanClass))) {
                    if (Modifier.isStatic(method.getModifiers())) {
                        if (log.isWarnEnabled()) {
                            log.warn("@" + getAnnotationType().getSimpleName() + " annotation is not supported on static methods: " + method);
                        }
                        return;
                    }
                    if (method.getParameterTypes().length == 0) {
                        if (log.isWarnEnabled()) {
                            log.warn("@" + getAnnotationType().getSimpleName() + " annotation should only be used on methods with parameters: " +
                                    method);
                        }
                    }
                    PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, beanClass);
                    elements.add(new AnnotatedMethodElement(method, pd, annotation));
                }
            }
        });

        return elements;

    }

    private Class<EtReference> getAnnotationType() {
        return EtReference.class;
    }

    private static <A extends Annotation> A getAnnotation(AnnotatedElement annotatedElement, Class<A> annotationType) {
        try {
            A annotation = annotatedElement.getAnnotation(annotationType);
            if (annotation == null) {
                for (Annotation metaAnn : annotatedElement.getAnnotations()) {
                    annotation = metaAnn.annotationType().getAnnotation(annotationType);
                    if (annotation != null) {
                        break;
                    }
                }
            }
            return synthesizeAnnotation(annotation, annotatedElement);
        }
        catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
            return null;
        }
    }

    private static <T> Collection<T> combine(Collection<? extends T>... elements) {
        List<T> allElements = new ArrayList<T>();
        for (Collection<? extends T> e : elements) {
            allElements.addAll(e);
        }
        return allElements;
    }

    public class AnnotatedInjectionMetadata extends InjectionMetadata{

        public AnnotatedInjectionMetadata(Class<?> targetClass, Collection<InjectionMetadata.InjectedElement> fieldElements,
                                          Collection<InjectionMetadata.InjectedElement> methodElements) {
            super(targetClass,  combine(fieldElements, methodElements));
        }

    }

    private AnnotatedInjectionMetadata buildAnnotatedMetadata(final Class<?> beanClass    ) {
        Collection<InjectionMetadata.InjectedElement> fieldElements = findFieldAnnotationMetadata(beanClass);
        Collection<InjectionMetadata.InjectedElement> methodElements = findAnnotatedMethodMetadata(beanClass);
        return new AnnotatedInjectionMetadata(beanClass, fieldElements, methodElements);
    }

    @Override
    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        if (beanType != null) {
            InjectionMetadata metadata = findInjectionMetadata(beanName, beanType, null);
            metadata.checkConfigMembers(beanDefinition);
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
        this.beanFactory.registerSingleton(EtRpcClient.class.getName(), etRpcClient(zkClientFactory()));
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
