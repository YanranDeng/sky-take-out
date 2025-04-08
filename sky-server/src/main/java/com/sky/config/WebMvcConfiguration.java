package com.sky.config;

import com.sky.interceptor.JwtTokenAdminInterceptor;
import com.sky.interceptor.JwtTokenUserInterceptor;
import com.sky.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        // 管理端
        registry.addInterceptor(jwtTokenAdminInterceptor)
                // 设置管理端拦截器的拦截路径
                .addPathPatterns("/admin/**")
                // 设置拦截路径白名单,管理端为登录路径不拦截
                .excludePathPatterns("/admin/employee/login");

        // 用户端
        registry.addInterceptor(jwtTokenUserInterceptor)
                // 设置用户端拦截器拦截路径
                .addPathPatterns("/user/**")
                // 设置拦截路径白名单,用户端由两个白名单路径,分别是微信小程序登录路径和微信小程序查询营业状态的路径,前者和管理端一致,后者是用户端特有的.
                .excludePathPatterns("/user/user/login")
                .excludePathPatterns("/user/shop/status");
    }


    /**
     * 通过knife4j生成接口文档
     *
     * @return
     */
    @Bean
    public Docket docket1() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("苍穹外卖项目接口文档")
                .version("2.0")
                .description("苍穹外卖项目接口文档")
                .build();
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .groupName("管理端接口")
                .select()
                // 将basePackage扫描包缩小精确到管理端对应的controller.admin
                .apis(RequestHandlerSelectors.basePackage("com.sky.controller.admin"))
                .paths(PathSelectors.any())
                .build();
        return docket;
    }


    /**
     * 在Docket对象创建时传入groupName属性,用于区分不同类的接口,
     * 本方法在原有的docket方法上将原有的docket方法分为两个,分别在创建Docket对象是赋予groupName不同值,
     * 且将basePackage属性的扫描包范围缩小到具体groupName所对应的包,以便于在swagger接口文档中将两个不同端(管理端和用户端)的接口区分展示.
     *
     * @return
     */
    @Bean
    public Docket docket2() {
        ApiInfo apiInfo = new ApiInfoBuilder()
                .title("苍穹外卖项目接口文档")
                .version("2.0")
                .description("苍穹外卖项目接口文档")
                .build();
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .groupName("用户端接口")
                .select()
                // 将basePackage扫描包缩小精确到用户端对应的controller.user
                .apis(RequestHandlerSelectors.basePackage("com.sky.controller.user"))
                .paths(PathSelectors.any())
                .build();
        return docket;
    }

    /**
     * 设置静态资源映射
     *
     * @param registry
     */
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 拓展MVC消息转化器,目前用于格式化后端响应给前端的实体类对象的时间属性
     * 重写WebMvcConfigurationSupport类的extendMessageConverters方法
     *
     * @param converters
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 创建一个消息转化器对象
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        // 创建一个对象转化器,并将该对象转化器传给消息转化器
        messageConverter.setObjectMapper(new JacksonObjectMapper());
        // 将自定义的消息转化器方法消息转化器列表的第一位上,Spring项目启动后会自动启用第一位的消息转化器(也即本自定义转化器)
        converters.add(0, messageConverter);
    }
}
