package com.github.smartretry.core;

/**
 * @author yuni[mn960mn@163.com]
 */
public interface RetryProcessor {

    /**
     * 执行定时重试
     */
    void doRetry();
}
