package com.github.smartretry.core;

/**
 * RetryHandler 注册器
 */
@FunctionalInterface
public interface RetryRegistry {

    void register(RetryHandler retryHandler);
}