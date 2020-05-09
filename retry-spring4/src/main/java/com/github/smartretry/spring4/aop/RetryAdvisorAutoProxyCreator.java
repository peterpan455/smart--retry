package com.github.smartretry.spring4.aop;

import lombok.Getter;
import lombok.Setter;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;

import java.util.List;

/**
 * 专用的AOP代理生成器
 *
 * @author yuni[mn960mn@163.com]
 */
public class RetryAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator {

    @Setter
    @Getter
    private List<Advisor> retryAdvisors;

    /**
     * 覆盖父类的获取Advisor的逻辑，父类是从Spring容器中获取所有的Advisor，这里使用实例本身的Advisor。
     * 这样就不用把smartretry专用的Advisor托管到Spring容器，避免被其他不相关的AbstractAdvisorAutoProxyCreator（比如说：AspectJAwareAdvisorAutoProxyCreator）找到并生成AOP代理
     *
     * @return
     */
    @Override
    public List<Advisor> findCandidateAdvisors() {
        return retryAdvisors;
    }
}
