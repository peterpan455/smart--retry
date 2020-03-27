package com.github.smartretry.spring4.registry.elasticjob;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.github.smartretry.core.RetryProcessor;

/**
 * @auther liuyang
 * @date 2020年03月27日
 */
public class RetryJob implements SimpleJob {

    private RetryProcessor retryProcessor;

    public RetryJob(RetryProcessor retryProcessor) {
        this.retryProcessor = retryProcessor;
    }

    @Override
    public void execute(ShardingContext shardingContext) {
        retryProcessor.doRetry();
    }
}