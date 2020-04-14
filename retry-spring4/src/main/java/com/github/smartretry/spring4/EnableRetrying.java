package com.github.smartretry.spring4;

import com.github.smartretry.spring4.autoconfigure.RetryImportSelector;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author yuni[mn960mn@163.com]
 * @see RetryImportSelector
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RetryImportSelector.class)
public @interface EnableRetrying {

    /**
     * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
     * to standard Java interface-based proxies. The default is {@code true}.
     */
    boolean proxyTargetClass() default false;

    /**
     * Indicate that the proxy should be exposed by the AOP framework as a {@code ThreadLocal}
     * for retrieval via the {@link org.springframework.aop.framework.AopContext} class.
     * Off by default, i.e. no guarantees that {@code AopContext} access will work.
     *
     * @since 1.3.6
     */
    boolean exposeProxy() default false;

    /**
     * <p>The default is {@link Ordered#LOWEST_PRECEDENCE} in order to run
     * after all other post-processors, so that it can add an advisor to
     * existing proxies rather than double-proxy.
     *
     * @since 1.3.6
     */
    int order() default Ordered.LOWEST_PRECEDENCE;
}