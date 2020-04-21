package com.github.smartretry.aspectj;

import com.github.smartretry.core.RetryHandler;
import com.github.smartretry.spring4.RetryHandlerRegistration;
import org.apache.commons.lang3.ArrayUtils;

/**
 * 切面类，处理RetryHandler实现
 *
 * @since 1.3.7
 */
public aspect RetryHandlerClassAspect {

    /**
     * 所有{@link RetryHandler 的实现类}
     */
    private pointcut isRetryHandler() : target(com.github.smartretry.core.RetryHandler);

    /**
     * 过滤掉框架里面的方法
     */
    private pointcut isInnerHandleMethod() : within(com.github.smartretry.core..*);

    /**
     * 名字为handle的方法
     */
    private pointcut isHandleMethod() : execution((java.lang.Object) *.handle(*));

    /**
     * 匹配所有{@link RetryHandler 的实现类}，对名字为handle的方法进行代理，但是过滤掉框架里面的名字为handle的方法
     */
    public pointcut matchClassMethod() : !isInnerHandleMethod() && isRetryHandler() && isHandleMethod();

    /**
     * Apply around advice to methods matching the {@link #matchClassMethod()} pointcut
     */
    Object around() : matchClassMethod() {
        RetryHandler retryHandler = (RetryHandler) thisJoinPoint.getTarget();
        Object[] args = thisJoinPoint.getArgs();
        return RetryHandlerRegistration.get(retryHandler.identity()).map(rh -> rh.handle(ArrayUtils.isEmpty(args) ? null : args[0])).orElse(null);
    }
}