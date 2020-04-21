package com.github.smartretry.spring4;

import com.github.smartretry.spring4.autoconfigure.RetryImportSelector;
import org.springframework.context.annotation.AdviceMode;
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
     * 默认使用代理的方式，也就意味着，在同一个类里面的方法调用，是不生效的（Spring AOP的实现机制导致）。如果想要在同一个方法内调用生效，请使用{@link AdviceMode#ASPECTJ}.
     * <p>
     * 如果配置{@link AdviceMode#ASPECTJ}，则
     * 1：下面的proxyTargetClass、exposeProxy、order3个属性值都会被忽略。
     * 2：需要加入  {@code retry-aspects} 依赖
     * 3：应用启动需要加上 -javaagent:/path/aspectjweaver-1.9.5.jar
     *
     * @since 1.3.7
     */
    AdviceMode mode() default AdviceMode.PROXY;

    /**
     * 使用JDK动态代理还是CGLIB生成代理。仅在mode=AdviceMode.PROXY生效
     */
    boolean proxyTargetClass() default true;

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