## 常见问题

### 1.1 访问Quartz-Job管理页面/job/dashboard.html 404
如果项目使用的是Springboot，一般没有这个问题。但是如果只是使用SpringMVC，则可能会有这个问题，解决方法如下
```java
@Configuration
public class AppWebMvcConfigurer extends WebMvcConfigurerAdapter {

    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/job/**").addResourceLocations("classpath:/public/job/");
    }
}
```