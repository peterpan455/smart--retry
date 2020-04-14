package com.github.smartretry.spring4.autoconfigure;

import com.github.smartretry.spring4.EnableRetrying;
import com.github.smartretry.spring4.EnvironmentConstants;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

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
        if (isRegisterProxyCreator()) {
            Map<String, Object> annotationData = importingClassMetadata.getAnnotationAttributes(EnableRetrying.class.getName());
            boolean proxyTargetClass = (Boolean) annotationData.get("proxyTargetClass");
            boolean exposeProxy = (Boolean) annotationData.get("exposeProxy");

            if (isSpringBoot() && springBootAopAuto()) {
                if (proxyTargetClass) {
                    AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
                }
                if (exposeProxy) {
                    AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);
                }
            } else {
                Integer order = (Integer) annotationData.get("order");
                registerAdvisorAutoProxyCreator(registry, proxyTargetClass, exposeProxy, order);
            }
        }
    }

    protected void registerAdvisorAutoProxyCreator(BeanDefinitionRegistry registry, boolean proxyTargetClass, boolean exposeProxy, Integer order) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(DefaultAdvisorAutoProxyCreator.class);
        beanDefinitionBuilder.addPropertyValue("proxyTargetClass", proxyTargetClass);
        beanDefinitionBuilder.addPropertyValue("exposeProxy", exposeProxy);
        beanDefinitionBuilder.addPropertyValue("order", order);
        beanDefinitionBuilder.addPropertyValue("usePrefix", true);
        beanDefinitionBuilder.addPropertyValue("advisorBeanNamePrefix", RetryAutoConfiguration.POINTCUT_ADVISOR_BEANNAME_PREFIX);
        registry.registerBeanDefinition("smartRetryAdvisorAutoProxyCreator", beanDefinitionBuilder.getBeanDefinition());
    }

    protected boolean isSpringBoot() {
        ClassLoader classLoader = getClass().getClassLoader();
        return ClassUtils.isPresent("org.springframework.boot.SpringApplication", classLoader)
                && ClassUtils.isPresent("org.springframework.boot.autoconfigure.SpringBootApplication", classLoader);
    }

    protected boolean springBootAopAuto() {
        return environment.getProperty("spring.aop.auto", Boolean.class, true);
    }

    protected boolean retryEnabled() {
        return environment.getProperty(EnvironmentConstants.RETRY_ENABLED, Boolean.class, true);
    }

    protected boolean retryWebEnabled() {
        return environment.getProperty(EnvironmentConstants.RETRY_WEB_ENABLED, Boolean.class, true);
    }

    protected boolean isRegisterProxyCreator() {
        return environment.getProperty(EnvironmentConstants.RETRY_REGISTER_PROXYCREATOR, Boolean.class, true);
    }
}