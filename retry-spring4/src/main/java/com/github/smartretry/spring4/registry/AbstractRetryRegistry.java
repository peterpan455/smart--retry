package com.github.smartretry.spring4.registry;

import com.github.smartretry.core.RetryHandler;
import com.github.smartretry.core.RetryRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractRetryRegistry implements RetryRegistry, BeanFactoryAware, EnvironmentAware {

    protected DefaultListableBeanFactory defaultListableBeanFactory;

    protected Environment environment;



    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
    }


}
