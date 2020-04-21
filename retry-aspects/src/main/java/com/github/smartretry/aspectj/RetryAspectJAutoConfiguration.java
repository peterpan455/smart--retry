package com.github.smartretry.aspectj;

import org.springframework.context.annotation.Bean;

/**
 * @author yuni[mn960mn@163.com]
 *
 * @since 1.3.7
 */
public class RetryAspectJAutoConfiguration {

    @Bean
    public RetryHandlerClassAspect retryHandlerClassAspect() {
        return RetryHandlerClassAspect.aspectOf();
    }

    @Bean
    public RetryHandlerAnnotationAspect retryHandlerAnnotationAspect() {
        return RetryHandlerAnnotationAspect.aspectOf();
    }
}