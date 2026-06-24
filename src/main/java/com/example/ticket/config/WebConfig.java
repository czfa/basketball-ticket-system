// src/main/java/com/example/ticket/config/WebConfig.java
package com.example.ticket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将根路径重定向到index.html
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
