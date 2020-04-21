package com.github.smartretry.spring4;

import com.github.smartretry.core.*;
import com.github.smartretry.core.impl.DefaultRetryHandlerPostProcessor;
import com.github.smartretry.core.impl.DefaultRetryProcessor;
import com.github.smartretry.core.impl.DefaultRetryTaskFactory;
import com.github.smartretry.core.impl.MethodRetryHandler;
import com.github.smartretry.core.listener.RetryListener;
import com.github.smartretry.core.util.RetryHandlerUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 对所有com.github.smartretry.core.RetryHandler和带有@RetryFunction注解的方法进行注册
 *
 * @author yuni[mn960mn@163.com]
 */
@Slf4j
public class RetryAnnotationBeanPostProcessor implements BeanPostProcessor, SmartInitializingSingleton, EnvironmentAware, InitializingBean, BeanFactoryAware {

    private DefaultListableBeanFactory defaultListableBeanFactory;

    private Environment environment;

    /**
     * 同一个类，如何在Spring容器中有多个，则会PostProcessor多次，这个Set就是为了防止多次注册的
     */
    private Set<Class<?>> postedClasseCache = new HashSet<>();

    private RetryRegistry retryRegistry;

    private RetrySerializer retrySerializer;

    private RetryTaskMapper retryTaskMapper;

    private RetryHandlerPostProcessor<Object, Object> retryHandlerPostProcessor;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        this.retryTaskMapper = defaultListableBeanFactory.getBean(RetryTaskMapper.class);
        this.retryRegistry = defaultListableBeanFactory.getBean(RetryRegistry.class);

        boolean beforeTask = environment.getProperty(EnvironmentConstants.RETRY_BEFORETASK, Boolean.class, Boolean.TRUE);
        this.retrySerializer = getRetrySerializerFromBeanFactory(defaultListableBeanFactory);
        if (this.retrySerializer == null) {
            this.retryHandlerPostProcessor = new DefaultRetryHandlerPostProcessor(retryTaskMapper, beforeTask);
        } else {
            this.retryHandlerPostProcessor = new DefaultRetryHandlerPostProcessor(new DefaultRetryTaskFactory(retrySerializer), retryTaskMapper, beforeTask);
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AopInfrastructureBean) {
            // Ignore AOP infrastructure such as scoped proxies.
            return bean;
        }

        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        if (!this.postedClasseCache.contains(targetClass)) {
            Object targetObject = AopUtils.isAopProxy(bean) ? AopProxyUtils.getSingletonTarget(bean) : bean;
            if (RetryHandler.class.isAssignableFrom(targetClass)) {
                RetryHandlerUtils.validateRetryHandler(targetClass);
                log.info("发现RetryHandler的实例：{}，准备注册", targetClass);
                registerJobBean((RetryHandler) targetObject);
                return bean;
            }
            ReflectionUtils.MethodFilter methodFilter = method -> method.getAnnotation(RetryFunction.class) != null;
            Set<Method> methods = MethodIntrospector.selectMethods(targetClass, methodFilter);
            methods.forEach(method -> processRetryFunction(targetObject, method));

            postedClasseCache.add(targetClass);
        }
        return bean;
    }

    protected void processRetryFunction(Object bean, Method method) {
        log.info("发现@RetryFunction的实例：{}，准备注册", method.toString());
        Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
        RetryHandlerUtils.validateRetryFunction(method);

        RetryFunction retryFunction = method.getAnnotation(RetryFunction.class);
        Supplier<RetryListener> retryListenerSupplier = () -> {
            RetryListener retryListener = null;
            String retryListenerName = retryFunction.retryListener();
            if (StringUtils.isNotBlank(retryListenerName)) {
                retryListener = defaultListableBeanFactory.getBean(retryListenerName, RetryListener.class);
            }
            return retryListener;
        };
        registerJobBean(new MethodRetryHandler(bean, invocableMethod, retryFunction, retryListenerSupplier));
    }

    private RetrySerializer getRetrySerializerFromBeanFactory(BeanFactory beanFactory) {
        try {
            return beanFactory.getBean(RetrySerializer.class);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    protected void registerJobBean(RetryHandler retryHandler) {
        if (retryHandler.identity().length() > 50) {
            throw new IllegalArgumentException("identity=" + retryHandler.identity() + " is too long, it must be less than 50");
        }

        RetryHandler retryHandlerProxy = retryHandlerPostProcessor.doPost(retryHandler);
        RetryHandlerRegistration.registry(retryHandlerProxy);

        RetryProcessor retryProcessor = new DefaultRetryProcessor(retryHandler, retryTaskMapper, retrySerializer);

        retryRegistry.register(retryHandler, retryProcessor);
    }

    @Override
    public void afterSingletonsInstantiated() {
        postedClasseCache.clear();
    }
}