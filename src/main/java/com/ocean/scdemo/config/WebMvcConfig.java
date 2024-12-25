package com.ocean.scdemo.config;

import com.ocean.scdemo.interceptor.CustomTestInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CustomTestInterceptor customTestInterceptor;

    public WebMvcConfig(CustomTestInterceptor customTestInterceptor) {
        this.customTestInterceptor = customTestInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(customTestInterceptor);
    }
}
