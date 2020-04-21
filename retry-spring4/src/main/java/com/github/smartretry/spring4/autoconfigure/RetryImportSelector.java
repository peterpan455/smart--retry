package com.github.smartretry.spring4.autoconfigure;

import com.github.smartretry.spring4.EnableRetrying;
import com.github.smartretry.spring4.EnvironmentConstants;
import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.AdviceMode;
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

    private static final String RETRY_ASPECT_CONFIGURATION_CLASS_NAME = "com.github.smartretry.aspectj.RetryAspectJAutoConfiguration";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (retryEnabled()) {
            Map<String, Object> annotationData = importingClassMetadata.getAnnotationAttributes(EnableRetrying.class.getName());

            AdviceMode adviceMode = AdviceMode.valueOf(annotationData.get("mode").toString());
            if (adviceMode == AdviceMode.PROXY) {
                registry.registerBeanDefinition(RetryProxyConfiguration.class.getName(), new RootBeanDefinition(RetryProxyConfiguration.class));
                registerProxyCreator(annotationData, registry);
            } else {
                Class<?> aspectJClass = getRetryAspectJConfigurationClass();
                registry.registerBeanDefinition(RETRY_ASPECT_CONFIGURATION_CLASS_NAME, new RootBeanDefinition(aspectJClass));
            }

            registry.registerBeanDefinition(RetryAutoConfiguration.class.getName(), new RootBeanDefinition(RetryAutoConfiguration.class));

            if (retryWebEnabled()) {
                registry.registerBeanDefinition(RetryWebAutoConfiguration.class.getName(), new RootBeanDefinition(RetryWebAutoConfiguration.class));
            }
        }
    }

    protected void registerProxyCreator(Map<String, Object> annotationData, BeanDefinitionRegistry registry) {
        if (isRegisterProxyCreator()) {
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
        beanDefinitionBuilder.addPropertyValue("advisorBeanNamePrefix", RetryProxyConfiguration.POINTCUT_ADVISOR_BEANNAME_PREFIX);
        registry.registerBeanDefinition("smartRetryAdvisorAutoProxyCreator", beanDefinitionBuilder.getBeanDefinition());
    }

    protected boolean isSpringBoot() {
        ClassLoader classLoader = getClass().getClassLoader();
        return ClassUtils.isPresent("org.springframework.boot.SpringApplication", classLoader)
                && ClassUtils.isPresent("org.springframework.boot.autoconfigure.SpringBootApplication", classLoader);
    }

    private Class<?> getRetryAspectJConfigurationClass() {
        try {
            return ClassUtils.forName(RETRY_ASPECT_CONFIGURATION_CLASS_NAME, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
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