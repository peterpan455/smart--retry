package com.github.smartretry.spring4.support;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * @author yuni[mn960mn@163.com]
 */
public class RetryConfigurationCondition implements ConfigurationCondition {

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.REGISTER_BEAN;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> map = metadata.getAnnotationAttributes(RetryConditional.class.getName());
        Class<?> missingBeanType = (Class<?>) map.get("missingBeanType");
        if (!Void.class.equals(missingBeanType)) {
            try {
                return !hasBean(context.getBeanFactory(), missingBeanType);
            } catch (NoSuchBeanDefinitionException e) {
                return true;
            }
        }
        Class<?> hasBeanType = (Class<?>) map.get("hasBeanType");
        if (!Void.class.equals(hasBeanType)) {
            try {
                return hasBean(context.getBeanFactory(), hasBeanType);
            } catch (NoUniqueBeanDefinitionException e) {
                return true;
            } catch (NoSuchBeanDefinitionException e) {
                return false;
            }
        }
        throw new IllegalArgumentException("至少要有一个Condition条件");
    }

    private boolean hasBean(ConfigurableListableBeanFactory beanFactory, Class<?> clazz) {
        if (beanFactory == null) {
            return false;
        }
        if (!beanFactory.getBeansOfType(clazz).isEmpty()) {
            return true;
        }

        BeanFactory parent = beanFactory.getParentBeanFactory();
        if (parent instanceof ConfigurableListableBeanFactory) {
            return hasBean((ConfigurableListableBeanFactory) parent, clazz);
        }

        return false;
    }
}