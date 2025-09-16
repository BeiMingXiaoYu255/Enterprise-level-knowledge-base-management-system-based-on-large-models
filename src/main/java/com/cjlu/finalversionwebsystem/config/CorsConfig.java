package com.cjlu.finalversionwebsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;



@Configuration
public class  CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建CORS配置对象
        CorsConfiguration config = new CorsConfiguration();
        // 允许的来源（生产环境建议明确指定域名，而非*）
        config.addAllowedOriginPattern("*"); // 支持通配符（如http://*.example.com）
        // 允许的请求头（如Content-Type、Authorization）
        config.addAllowedHeader("*");
        // 允许的HTTP方法（GET、POST、PUT、DELETE等）
        config.addAllowedMethod("*");
        // 是否允许携带Cookie（前端请求需设置withCredentials: true）
        config.setAllowCredentials(true);
        // 预检请求的缓存时间（秒），减少重复验证
        config.setMaxAge(3600L);

        // 2. 配置适用的路径（所有路径）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // /**表示所有接口

        // 3. 创建并返回过滤器
        return new CorsFilter(source);
    }
}
