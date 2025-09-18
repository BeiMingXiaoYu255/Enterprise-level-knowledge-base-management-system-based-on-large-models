package com.cjlu.finalversionwebsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class WebSecurityConfig {

    @Bean
    public OncePerRequestFilter cspHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, javax.servlet.FilterChain filterChain) throws javax.servlet.ServletException, IOException {
                // 允许所有请求通过
                filterChain.doFilter(request, response);
            }
        };
    }

}
