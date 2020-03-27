package com.github.smartretry.spring4.registry.elasticjob;

import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.github.smartretry.core.RetryHandler;
import com.github.smartretry.core.RetryProcessor;
import com.github.smartretry.spring4.registry.AbstractRetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ElasticJobRegistry extends AbstractRetryRegistry implements InitializingBean {

    /**
     * 默认不分片
     */
    public static final int DEFAULT_SHARDINGTOTALCOUNT = 1;

    private CoordinatorRegistryCenter registryCenter;

    private JobEventConfiguration jobEventConfiguration;

    protected AtomicInteger jobBeanNameIndex = new AtomicInteger(0);

    private ElasticJobListener[] elasticJobListeners = new ElasticJobListener[0];

    @Override
    public void afterPropertiesSet() {
        this.registryCenter = defaultListableBeanFactory.getBean(CoordinatorRegistryCenter.class);

        try {
            this.jobEventConfiguration = defaultListableBeanFactory.getBean(JobEventConfiguration.class);
        } catch (NoUniqueBeanDefinitionException e) {
            throw e;
        } catch (NoSuchBeanDefinitionException e) {
            //ignore
        }
    }

    @Override
    public void register(RetryHandler retryHandler, RetryProcessor retryProcessor) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(SpringJobScheduler.class);
        beanDefinitionBuilder.addConstructorArgValue(new RetryJob(retryProcessor));
        beanDefinitionBuilder.addConstructorArgValue(registryCenter);
        beanDefinitionBuilder.addConstructorArgValue(createLiteJobConfiguration(retryHandler));
        if (jobEventConfiguration != null) {
            beanDefinitionBuilder.addConstructorArgValue(jobEventConfiguration);
        }
        beanDefinitionBuilder.addConstructorArgValue(elasticJobListeners);
        beanDefinitionBuilder.setInitMethodName("init");

        String jobBeanName = getJobBeanName(retryHandler);
        defaultListableBeanFactory.registerBeanDefinition(jobBeanName, beanDefinitionBuilder.getBeanDefinition());

        //此处的getBean调用是为了手工触发Bean的初始化
        defaultListableBeanFactory.getBean(jobBeanName);
        log.info("identity={}已成功注册到Elastic-Job", retryHandler.identity());
    }

    protected LiteJobConfiguration createLiteJobConfiguration(RetryHandler retryHandler) {
        JobCoreConfiguration jobConfig = createJobCoreConfiguration(retryHandler);
        return LiteJobConfiguration.newBuilder(new SimpleJobConfiguration(jobConfig, retryHandler.identity())).disabled(!retryHandler.autoStartup()).build();
    }

    protected JobCoreConfiguration createJobCoreConfiguration(RetryHandler retryHandler) {
        return JobCoreConfiguration.newBuilder(retryHandler.identity(), retryHandler.cron(), DEFAULT_SHARDINGTOTALCOUNT).description(retryHandler.name()).build();
    }

    protected String getJobBeanName(RetryHandler retryHandler) {
        return "job." + retryHandler.identity() + "." + jobBeanNameIndex.incrementAndGet();
    }
}