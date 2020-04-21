## 整合Elastic-Job

[Elastic-Job](http://elasticjob.io)是一个分布式调度解决方案，由两个相互独立的子项目Elastic-Job-Lite和Elastic-Job-Cloud组成。Elastic-Job-Lite定位为轻量级无中心化解决方案，使用jar包的形式提供分布式任务的协调服务；Elastic-Job-Cloud采用自研Mesos Framework的解决方案，额外提供资源治理、应用分发以及进程隔离等功能。
本系统整合的是Elastic-Job-Lite


### 使用方式

* 加入Elastic-Job-Lite的依赖
```xml
<dependency>
    <groupId>com.dangdang</groupId>
    <artifactId>elastic-job-lite-spring</artifactId>
    <version>2.1.5</version>
</dependency>
```

* 安装apache zookeeper并启动
* 在Spring容器中托管如下2个bean
```java
@Bean(initMethod = "init", destroyMethod = "close")
public CoordinatorRegistryCenter coordinatorRegistryCenter() {
    return new ZookeeperRegistryCenter(new ZookeeperConfiguration("192.168.1.100:2181", "orderNamespace"));
}

@Bean
public ElasticJobRegistry elasticJobRegistry() {
    return new ElasticJobRegistry();
}
```
* 接下来就可以在elastic-job-lite-console上面来管理重试Job了

PS： 最后要注意，使用ElasticJobRegistry必须要指定@RetryFunction或RetryHandler的cron表达式