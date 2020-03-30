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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 把重试任务注册到Elastic-Job
 *
 * @author yuni[mn960mn@163.com]
 * @since 1.3.5
 */
@Slf4j
public class ElasticJobRegistry extends AbstractRetryRegistry implements InitializingBean {

    /**
     * 默认不分片
     */
    public static final int DEFAULT_SHARDINGTOTALCOUNT = 1;

    private CoordinatorRegistryCenter registryCenter;

    private JobEventConfiguration jobEventConfiguration;

    protected AtomicInteger jobBeanNameIndex = new AtomicInteger(0);

    protected ElasticJobListener[] elasticJobListeners = new ElasticJobListener[0];

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
        if (StringUtils.isBlank(retryHandler.cron())) {
            throw new IllegalArgumentException("identity=" + retryHandler.identity() + ", 使用Elastic-Job注册器，必须指定RetryHandler/RetryFunction的cron表达式");
        }
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(SpringJobScheduler.class);
        beanDefinitionBuilder.addConstructorArgValue(new RetryJob(retryProcessor));
        beanDefinitionBuilder.addConstructorArgValue(registryCenter);
        beanDefinitionBuilder.addConstructorArgValue(createLiteJobConfiguration(retryHandler));
        if (jobEventConfiguration != null) {
            beanDefinitionBuilder.addConstructorArgValue(jobEventConfiguration);
        }
        beanDefinitionBuilder.addConstructorArgValue(getElasticJobListeners());
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

    protected ElasticJobListener[] getElasticJobListeners() {
        return this.elasticJobListeners;
    }
}