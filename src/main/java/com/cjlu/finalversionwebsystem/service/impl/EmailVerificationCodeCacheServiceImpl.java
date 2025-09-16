package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.service.Interface.EmailVerificationCodeCacheServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmailVerificationCodeCacheServiceImpl implements EmailVerificationCodeCacheServiceInterface {

    @Autowired
    private StringRedisTemplate redisTemplate; // 注入StringRedisTemplate

    private static final String VERIFICATION_CODE_KEY = "verification:code:"; // 验证码的key前缀
    private static final long EXPIRE_TIME = 5; // 验证码的有效期，单位为分钟

    public void saveCode(String email, String code) {
        log.info("Saving verification code for email: {}", email);
        try {
            redisTemplate.opsForValue().set( // 将验证码存入Redis
                    VERIFICATION_CODE_KEY + email, // key为email加上前缀
                    code, // value为验证码
                    EXPIRE_TIME, // 设置有效期
                    TimeUnit.MINUTES // 时间单位为分钟
            );
            log.info("Verification code saved for email: {}", email);
        } catch (Exception e) {
            // 处理异常
            log.error("Error saving verification code for email: {}", email, e);
        }
    }

    public boolean validateCode(String email, String inputCode) {
        log.info("Validating verification code for email: {}", email);
        try {
            String key = VERIFICATION_CODE_KEY + email; // 构建key
            String storedCode = redisTemplate.opsForValue().get(key); // 从Redis中获取存储的验证码
            log.info("Stored code for email {}: {}", email, storedCode);

            if (storedCode == null) { // 如果存储的验证码为空，返回false
                log.info("No stored code found for email: {}", email);
                return false;
            }

            boolean isValid = storedCode.equals(inputCode); // 比较输入的验证码和存储的验证码
            if (isValid) {
                redisTemplate.delete(key); // 如果验证码有效，删除Redis中的验证码
                log.info("Validation successful for email: {}", email);
            } else {
                log.info("Validation failed for email: {}", email);
            }
            return isValid; // 返回验证结果
        } catch (Exception e) {
            // 处理异常
            log.error("Error validating verification code for email: {}", email, e);
            return false;
        }
    }
}