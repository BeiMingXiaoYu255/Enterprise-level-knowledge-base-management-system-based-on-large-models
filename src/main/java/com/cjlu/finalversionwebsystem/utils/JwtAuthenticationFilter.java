package com.cjlu.finalversionwebsystem.utils;

import com.cjlu.finalversionwebsystem.service.impl.JwtBlacklistServiceImpl;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JwtAuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    // 不需要认证的URL白名单
    private final List<String> excludeUrls = Arrays.asList(
            "/user/**",
            "/ai/**",
            "/KLB/**",
            "/**"
    );

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private JwtBlacklistServiceImpl blacklistService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化代码（如加载配置）
        logger.info("JwtAuthenticationFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String token = extractToken(httpRequest);
        // 检查黑名单
        if (token != null && blacklistService.isBlacklisted(token)) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token revoked");
            return;
        }
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        logger.debug("Processing request to {}", requestURI);

        // 检查是否为排除的URL
        if (isExcluded(requestURI)) {
            logger.debug("Excluded URL: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        // 从请求头中获取JWT令牌
        token = extractToken(httpRequest);

        try {
            // 检查令牌是否存在
            if (token == null) {
                logger.warn("Missing token for {}", requestURI);
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authentication token");
                return;
            }

            // 检查令牌是否在黑名单中
            if (isTokenBlacklisted(token)) {
                logger.warn("Blacklisted token for {}", requestURI);
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been revoked");
                return;
            }

            // 验证令牌有效性
            if (jwtUtils.validateToken(token)) {
                logger.debug("Valid token provided for {}", requestURI);

                // 将用户信息存入Request属性
                String username = jwtUtils.getUsernameFromToken(token);
                httpRequest.setAttribute("username", username);

                chain.doFilter(request, response);
            } else {
                logger.warn("Invalid token for {}", requestURI);
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication token");
            }
        } catch (Exception e) {
            logger.error("Error processing request for {}", requestURI, e);
            httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    @Override
    public void destroy() {
        // 清理资源
        logger.info("JwtAuthenticationFilter destroyed");
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    public void addToBlacklist(String token) {
        try {
            // 获取token剩余有效期
            long expiration = jwtUtils.getTokenExpiration(token) - System.currentTimeMillis();
            if (expiration > 0) {
                // 使用Redis存储黑名单，键为token，值为任意字符串，设置过期时间
                String key = BLACKLIST_KEY_PREFIX + token;
                redisTemplate.opsForValue().set(key, "revoked", expiration, TimeUnit.MILLISECONDS);
                logger.info("Token added to blacklist, expires in {}ms", expiration);
            }
        } catch (Exception e) {
            logger.error("Failed to add token to blacklist", e);
        }
    }

    private boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_KEY_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private boolean isExcluded(String requestURI) {
        // 使用Ant风格路径匹配（需要额外依赖或自定义实现）
        for (String pattern : excludeUrls) {
            if (isPathMatch(pattern, requestURI)) {
                return true;
            }
        }
        return false;
    }

    // 简化的Ant风格路径匹配实现
    private boolean isPathMatch(String pattern, String path) {
        if (pattern.endsWith("/**")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 3));
        }
        return pattern.equals(path);
    }

    public void logout(String token) {
        long expiration = jwtUtils.getTokenExpiration(token) - System.currentTimeMillis();
        if (expiration > 0) {
            blacklistService.addToBlacklist(token, expiration);
        }
    }
}