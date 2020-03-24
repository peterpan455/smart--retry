package com.github.smartretry.spring4;

import com.github.smartretry.core.*;
import com.github.smartretry.core.impl.MethodRetryHandler;
import com.github.smartretry.core.listener.RetryListener;
import com.github.smartretry.core.util.RetryHandlerUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 对所有com.github.smartretry.core.RetryHandler和带有@RetryFunction注解的方法注册为Quartz Job
 *
 * @author yuni[mn960mn@163.com]
 */
@Slf4j
public class RetryAnnotationBeanPostProcessor implements BeanPostProcessor, SmartInitializingSingleton, BeanFactoryAware {

    private DefaultListableBeanFactory defaultListableBeanFactory;

    private Set<Class<?>> nonAnnotatedClasses = new HashSet<>();

    private RetryRegistry retryRegistry;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        retryRegistry = beanFactory.getBean(RetryRegistry.class);
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
        if (!this.nonAnnotatedClasses.contains(targetClass)) {
            Object targetObject = AopProxyUtils.getSingletonTarget(bean);
            if (RetryHandler.class.isAssignableFrom(targetClass)) {
                RetryHandlerUtils.validateRetryHandler(targetClass);
                log.info("发现RetryHandler的实例：{}，准备注册", targetClass);
                registerJobBean((RetryHandler) targetObject);
                return bean;
            }
            ReflectionUtils.MethodFilter methodFilter = method -> method.getAnnotation(RetryFunction.class) != null;
            Set<Method> methods = MethodIntrospector.selectMethods(targetClass, methodFilter);
            methods.forEach(method -> processRetryFunction(targetObject, method));
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

    private void registerJobBean(RetryHandler retryHandler) {
        retryRegistry.register(retryHandler);
    }

    @Override
    public void afterSingletonsInstantiated() {
        nonAnnotatedClasses.clear();
    }
}