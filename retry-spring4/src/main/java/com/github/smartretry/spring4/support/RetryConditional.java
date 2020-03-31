package com.github.smartretry.spring4.support;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 考虑到实际使用的可能不是springboot环境，所以才有此类。类似springboot中的@ConditionalOnMissingBean、@ConditionalOnBean
 *
 * @author yuni[mn960mn@163.com]
 * @see RetryConfigurationCondition
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(RetryConfigurationCondition.class)
public @interface RetryConditional {

    Class<?> missingBeanType() default Void.class;

    Class<?> hasBeanType() default Void.class;
}
