package com.cjlu.finalversionwebsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 关键修复：当允许Cookie时，不能使用*，必须指定具体的前端域名
        // 替换为你的前端实际域名，这里适配你的127.0.0.1:5500
        config.addAllowedOrigin("http://127.0.0.1:5500");
        // 如果有多个前端域名，依次添加
        // config.addAllowedOrigin("http://localhost:8080");

        // 允许的请求头
        config.setAllowedHeaders(Arrays.asList(
                "Origin", "Content-Type", "Accept", "Authorization",
                "Cookie", "X-Requested-With"
        ));

        // 允许的HTTP方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许携带Cookie
        config.setAllowCredentials(true);

        // 暴露响应头
        config.setExposedHeaders(Arrays.asList("Set-Cookie", "Cookie"));

        // 预检请求缓存时间
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
