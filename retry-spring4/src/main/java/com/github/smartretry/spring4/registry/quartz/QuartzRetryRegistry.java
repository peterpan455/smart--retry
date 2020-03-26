package com.github.smartretry.spring4.registry.quartz;

import com.github.smartretry.core.*;
import com.github.smartretry.core.impl.DefaultRetryProcessor;
import com.github.smartretry.spring4.*;
import com.github.smartretry.spring4.admin.model.JobStatusEnum;
import com.github.smartretry.spring4.job.RetryJob;
import com.github.smartretry.spring4.job.RetryJobFactory;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.spi.JobFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.OrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 把重试任务注册到quartz中
 */
public class QuartzRetryRegistry implements RetryRegistry, BeanFactoryAware, EnvironmentAware, InitializingBean, DisposableBean {

    private DefaultListableBeanFactory defaultListableBeanFactory;

    private Environment environment;

    private Executor taskExecutor = Executors.newCachedThreadPool();

    private List<RetryBeanDefinitionBuilderCustomizer> retryBeanDefinitionBuilderCustomizers;

    private AtomicInteger jobNameIndex = new AtomicInteger(0);

    private JobFactory jobFactory = new RetryJobFactory();

    /**
     * job是否自动启动。如果配置的是false，就需要手工启动
     */
    private boolean jobAutoStartup;

    /**
     * 延迟多少秒之后，再启动job
     */
    private int jobStartupDelay;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        this.jobAutoStartup = environment.getProperty(EnvironmentConstants.RETRY_JOB_AUTOSTARTUP, Boolean.class, Boolean.TRUE);
        this.jobStartupDelay = environment.getProperty(EnvironmentConstants.RETRY_JOB_STARTUPDELAY, Integer.class, 30);

        this.retryBeanDefinitionBuilderCustomizers = new ArrayList<>(defaultListableBeanFactory.getBeansOfType(RetryBeanDefinitionBuilderCustomizer.class).values());
        this.retryBeanDefinitionBuilderCustomizers.sort(OrderComparator.INSTANCE);

        if (defaultListableBeanFactory.containsBean(BeanConstants.DEFAULT_RETRY_TASKEXECUTOR)) {
            this.taskExecutor = defaultListableBeanFactory.getBean(BeanConstants.DEFAULT_RETRY_TASKEXECUTOR, Executor.class);
        }
    }

    @Override
    public void register(RetryHandler retryHandler, RetryProcessor retryProcessor) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(JobConstant.JOB_INSTANCE_KEY, new RetryJob(retryProcessor));

        int index = jobNameIndex.incrementAndGet();

        String group = JobConstant.JOB_GROUP_KEY + "_" + index;
        String name = JobConstant.JOB_NAME_KEY + "_" + index;
        String triggerName = JobConstant.JOB_TRIGGER_NAME_KEY + "_" + index;

        JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
        jobDetailFactoryBean.setJobClass(RetryJob.class);
        jobDetailFactoryBean.setName(name);
        jobDetailFactoryBean.setGroup(group);
        jobDetailFactoryBean.setJobDataMap(jobDataMap);
        jobDetailFactoryBean.afterPropertiesSet();

        Object jobTrigger;
        String jobPeriod;

        if (StringUtils.isNotBlank(retryHandler.cron())) {
            jobPeriod = retryHandler.cron();

            CronTriggerFactoryBean cronTriggerFactoryBean = new CronTriggerFactoryBean();
            cronTriggerFactoryBean.setCronExpression(retryHandler.cron());
            cronTriggerFactoryBean.setName(triggerName);
            cronTriggerFactoryBean.setGroup(group);
            cronTriggerFactoryBean.setJobDataMap(jobDataMap);
            cronTriggerFactoryBean.setJobDetail(jobDetailFactoryBean.getObject());

            try {
                cronTriggerFactoryBean.afterPropertiesSet();
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }

            jobTrigger = cronTriggerFactoryBean.getObject();
        } else {
            jobPeriod = Integer.toString(retryHandler.interval());

            SimpleTriggerFactoryBean simpleTriggerFactoryBean = new SimpleTriggerFactoryBean();
            simpleTriggerFactoryBean.setRepeatInterval(retryHandler.interval() * 1000L);
            simpleTriggerFactoryBean.setName(triggerName);
            simpleTriggerFactoryBean.setGroup(group);
            simpleTriggerFactoryBean.setJobDataMap(jobDataMap);
            simpleTriggerFactoryBean.setJobDetail(jobDetailFactoryBean.getObject());
            simpleTriggerFactoryBean.afterPropertiesSet();

            jobTrigger = simpleTriggerFactoryBean.getObject();
        }

        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(RetrySchedulerFactoryBean.class);
        beanDefinitionBuilder.addPropertyValue("taskExecutor", taskExecutor);
        beanDefinitionBuilder.addPropertyValue("triggers", jobTrigger);
        beanDefinitionBuilder.addPropertyValue("autoStartup", jobAutoStartup);
        beanDefinitionBuilder.addPropertyValue("startupDelay", jobStartupDelay);
        beanDefinitionBuilder.addPropertyValue("jobFactory", jobFactory);
        beanDefinitionBuilder.addPropertyValue("jobGroup", group);
        beanDefinitionBuilder.addPropertyValue("jobName", retryHandler.name());
        beanDefinitionBuilder.addPropertyValue("jobIdentity", retryHandler.identity());
        beanDefinitionBuilder.addPropertyValue("jobPeriod", jobPeriod);
        if (!jobAutoStartup) {
            beanDefinitionBuilder.addPropertyValue("jobStatusEnum", JobStatusEnum.PREPARE);
        }

        //执行用户自定义的后置处理逻辑
        retryBeanDefinitionBuilderCustomizers.forEach(c -> c.customize(retryHandler.identity(), beanDefinitionBuilder));

        // 注册Bean
        String jobBeanName = "JOB." + index + "." + retryHandler.identity();
        defaultListableBeanFactory.registerBeanDefinition(jobBeanName, beanDefinitionBuilder.getBeanDefinition());
    }

    @Override
    public void destroy() {
        if (taskExecutor instanceof ExecutorService) {
            ((ExecutorService) taskExecutor).shutdown();
        }
    }
}