package com.github.smartretry.spring4.registry.elasticjob;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.github.smartretry.core.RetryProcessor;

/**
 * @author yuni[mn960mn@163.com]
 * @since 1.3.5
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