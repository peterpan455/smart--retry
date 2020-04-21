package com.github.smartretry.aspectj;

import com.github.smartretry.core.RetryFunction;
import com.github.smartretry.core.RetryHandler;
import com.github.smartretry.core.util.RetryHandlerUtils;
import com.github.smartretry.spring4.RetryHandlerRegistration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

/**
 * 切面类，处理@RetryFunction注解
 *
 * @since 1.3.7
 */
public aspect RetryHandlerAnnotationAspect {

    /**
     * 带有@RetryFunction注解，且有且只有一个参数
     */
    private pointcut hasAnnotation() : execution(@com.github.smartretry.core.RetryFunction * *(*));

    /**
     * 过滤掉框架里面的方法
     */
    private pointcut isInnerHandleMethod() : within(com.github.smartretry.core..*);

    /**
     * 参数是{@link Iterable}类型
     */
    private pointcut paramIsIterable() : args(java.lang.Iterable);

    /**
     * 参数是{@link Map}类型
     */
    private pointcut paramIsMap() : args(java.util.Map);

    /**
     * 方法必须带有@RetryFunction注解，有一个参数，且参数类型不能是 {@link Iterable}、{@link Map} 类型
     */
    public pointcut matchAnnotationMethod() : !isInnerHandleMethod() && hasAnnotation() && !(paramIsIterable() || paramIsMap());

    /**
     * Apply around advice to methods matching the {@link #matchAnnotationMethod()} pointcut
     */
    Object around() : matchAnnotationMethod() {
        Signature signature = thisJoinPointStaticPart.getSignature();
        if (signature instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) signature;
            Method method = methodSignature.getMethod();
            RetryFunction retryFunction = method.getAnnotation(RetryFunction.class);
            Object[] args = thisJoinPoint.getArgs();
            String identity = retryFunction.identity();
            if (StringUtils.isBlank(identity)) {
                identity = RetryHandlerUtils.getMethodIdentity(method);
            }
            Optional<RetryHandler> optional = RetryHandlerRegistration.get(identity);
            if (optional.isPresent()) {
                return optional.get().handle(ArrayUtils.isEmpty(args) ? null : args[0]);
            }
            throw new IllegalArgumentException("找不到对应的RetryHandler代理，identity=" + identity);
        }
        throw new IllegalArgumentException("找不到对应的RetryHandler代理，signature=" + signature);
    }
}
