package com.github.smartretry.spring4.autoconfigure;

import com.github.smartretry.spring4.EnableRetrying;
import com.github.smartretry.spring4.EnvironmentConstants;
import com.github.smartretry.spring4.aop.RetryAdvisorAutoProxyCreator;
import com.github.smartretry.spring4.aop.RetryHandlerClassInterceptor;
import com.github.smartretry.spring4.aop.RetryHandlerClassPointcut;
import com.github.smartretry.spring4.aop.RetryHandlerMethodInterceptor;
import com.github.smartretry.spring4.aop.RetryHandlerMethodPointcut;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 根据配置来决定是否进行自动配置
 *
 * @author yuni[mn960mn@163.com]
 */
public class RetryImportSelector implements EnvironmentAware, ImportBeanDefinitionRegistrar {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (retryEnabled()) {
            registerProxyCreator(importingClassMetadata, registry);

            registry.registerBeanDefinition(RetryAutoConfiguration.class.getName(), new RootBeanDefinition(RetryAutoConfiguration.class));

            if (retryWebEnabled()) {
                registry.registerBeanDefinition(RetryWebAutoConfiguration.class.getName(), new RootBeanDefinition(RetryWebAutoConfiguration.class));
            }
        }
    }

    protected void registerProxyCreator(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> annotationData = importingClassMetadata.getAnnotationAttributes(EnableRetrying.class.getName());
        boolean proxyTargetClass = (Boolean) annotationData.get("proxyTargetClass");
        boolean exposeProxy = (Boolean) annotationData.get("exposeProxy");
        Integer order = (Integer) annotationData.get("order");
        registerAdvisorAutoProxyCreator(registry, proxyTargetClass, exposeProxy, order);
    }

    protected void registerAdvisorAutoProxyCreator(BeanDefinitionRegistry registry, boolean proxyTargetClass, boolean exposeProxy, Integer order) {
        List<Advisor> retryAdvisors = new ArrayList<>();
        retryAdvisors.add(new DefaultPointcutAdvisor(new RetryHandlerMethodPointcut(), new RetryHandlerMethodInterceptor()));
        retryAdvisors.add(new DefaultPointcutAdvisor(new RetryHandlerClassPointcut(), new RetryHandlerClassInterceptor()));

        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(RetryAdvisorAutoProxyCreator.class);
        beanDefinitionBuilder.addPropertyValue("proxyTargetClass", proxyTargetClass);
        beanDefinitionBuilder.addPropertyValue("exposeProxy", exposeProxy);
        beanDefinitionBuilder.addPropertyValue("order", order);
        beanDefinitionBuilder.addPropertyValue("retryAdvisors", retryAdvisors);

        registry.registerBeanDefinition("smartRetryAdvisorAutoProxyCreator", beanDefinitionBuilder.getBeanDefinition());
    }

    protected boolean retryEnabled() {
        return environment.getProperty(EnvironmentConstants.RETRY_ENABLED, Boolean.class, true);
    }

    protected boolean retryWebEnabled() {
        return environment.getProperty(EnvironmentConstants.RETRY_WEB_ENABLED, Boolean.class, true);
    }
}