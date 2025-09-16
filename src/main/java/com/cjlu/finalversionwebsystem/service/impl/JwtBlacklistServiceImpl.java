package com.cjlu.finalversionwebsystem.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class JwtBlacklistServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(JwtBlacklistServiceImpl.class);
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 将JWT添加到黑名单
     *
     * @param token            JWT令牌
     * @param expirationMillis 过期时间（毫秒）
     * @throws IllegalArgumentException 如果token为空或过期时间非正
     */
    public void addToBlacklist(String token, long expirationMillis) {
        // 参数校验
        if (token == null || token.isEmpty()) {
            logger.error("尝试添加空token到黑名单");
            throw new IllegalArgumentException("token不能为空");
        }
        if (expirationMillis <= 0) {
            logger.error("无效的过期时间: {}", expirationMillis);
            throw new IllegalArgumentException("过期时间必须为正数");
        }

        String key = getBlacklistKey(token);
        try {
            // 将token加入黑名单，设置过期时间
            redisTemplate.opsForValue().set(key, "revoked", expirationMillis, TimeUnit.MILLISECONDS);
            logger.info("Token已加入黑名单: {}", maskToken(token));
        } catch (RedisSystemException e) {
            // 记录异常并抛出运行时异常
            logger.error("添加token到黑名单失败: {}", token, e);
            throw new RuntimeException("Redis操作失败", e);
        }
    }

    /**
     * 检查JWT是否在黑名单中
     *
     * @param token JWT令牌
     * @return 如果token在黑名单中返回true，否则返回false
     * @throws IllegalArgumentException 如果token为空
     */
    public boolean isBlacklisted(String token) {
        // 参数校验
        if (token == null || token.isEmpty()) {
            logger.error("尝试检查空token是否在黑名单中");
            throw new IllegalArgumentException("token不能为空");
        }

        String key = getBlacklistKey(token);
        try {
            // 检查token是否在黑名单中
            Boolean result = redisTemplate.hasKey(key);
            logger.debug("Token {} 是否在黑名单中: {}", maskToken(token), result);
            return Boolean.TRUE.equals(result);
        } catch (RedisSystemException e) {
            // Redis异常时记录错误并默认视为在黑名单中（安全优先）
            logger.error("检查token黑名单失败，默认视为已黑名单: {}", token, e);
            return true;
        }
    }

    /**
     * 生成Redis键名
     */
    private String getBlacklistKey(String token) {
        return BLACKLIST_KEY_PREFIX + token;
    }

    /**
     * 掩码处理JWT，只显示前8位和后4位，保护敏感信息
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 12) {
            return token;
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}