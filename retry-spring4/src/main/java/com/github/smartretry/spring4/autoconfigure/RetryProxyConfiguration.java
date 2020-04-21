package com.github.smartretry.spring4.autoconfigure;

import com.github.smartretry.spring4.aop.RetryHandlerClassInterceptor;
import com.github.smartretry.spring4.aop.RetryHandlerClassPointcut;
import com.github.smartretry.spring4.aop.RetryHandlerAnnotationInterceptor;
import com.github.smartretry.spring4.aop.RetryHandlerAnnotationPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;

/**
 * @author yuni[mn960mn@163.com]
 */
public class RetryProxyConfiguration {

    public static final String POINTCUT_ADVISOR_BEANNAME_PREFIX = "smartRetryPointcutAdvisorMatch";

    @Bean(POINTCUT_ADVISOR_BEANNAME_PREFIX + "Annotation")
    public DefaultPointcutAdvisor matchAnnotationPointcutAdvisor() {
        return new DefaultPointcutAdvisor(new RetryHandlerAnnotationPointcut(), new RetryHandlerAnnotationInterceptor());
    }

    @Bean(POINTCUT_ADVISOR_BEANNAME_PREFIX + "Class")
    public DefaultPointcutAdvisor matchClassPointcutAdvisor() {
        return new DefaultPointcutAdvisor(new RetryHandlerClassPointcut(), new RetryHandlerClassInterceptor());
    }
}
