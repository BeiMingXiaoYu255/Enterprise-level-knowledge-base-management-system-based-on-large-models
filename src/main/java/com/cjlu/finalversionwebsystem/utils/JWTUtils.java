package com.cjlu.finalversionwebsystem.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JWTUtils {
    private static final Logger log = LoggerFactory.getLogger(JWTUtils.class);

    // 密钥（建议生产环境使用更长更复杂的密钥，并通过配置文件注入）
    private static final String SECRET_KEY = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    // 过期时间：2小时
    private static final long EXPIRATION_TIME = 7_200_000;
    // JWT发行人
    private static final String ISSUER = "BeiMingXiaoYu";

    /**
     * 生成JWT Token
     * @param username 用户唯一标识（不能为null或空）
     * @param claims 自定义声明(可选)
     * @return JWT字符串
     */
    public static String createToken(String username, Map<String, Object> claims) {
        // 校验用户名非空
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为null或空字符串");
        }

        if (claims == null) {
            claims = new HashMap<>();
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuer(ISSUER)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析JWT Token并获取Claims
     * @param token JWT字符串（不能为null或空）
     * @return 包含声明的Claims对象
     * @throws JwtException 如果token无效、已过期或为空
     */
    public static Claims parseToken(String token) throws JwtException {
        // 增加token空值校验
        if (token == null || token.trim().isEmpty()) {
            log.error("尝试解析空的JWT token");
            throw new JwtException("JWT token不能为空");
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.error("JWT解析失败: {}", e.getMessage());
            throw e; // 重新抛出异常让调用方处理
        }
    }

    /**
     * 验证JWT Token是否有效
     * @param token JWT字符串
     * @return 如果有效返回true，否则返回false
     */
    public static boolean validateToken(String token) {
        // 先校验token是否为空
        if (token == null || token.trim().isEmpty()) {
            log.warn("验证空的JWT token");
            return false;
        }

        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从JWT Token中获取用户名
     * @param token JWT字符串
     * @return 用户名，如果token无效则返回null
     */
    public static String getUsernameFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("从空token中获取用户名");
            return null;
        }

        try {
            return parseToken(token).getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * 生成签名密钥
     * @return 用于签名的SecretKeySpec对象
     */
    private static SecretKeySpec getSigningKey() {
        // 检查密钥是否符合Base64编码要求
        try {
            byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
            return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
        } catch (IllegalArgumentException e) {
            log.error("密钥Base64解码失败，请检查密钥是否正确", e);
            throw e;
        }
    }

    public static long getTokenExpiration(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("获取空token的过期时间");
            return -1;
        }

        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().getTime();
        } catch (JwtException e) {
            return -1;
        }
    }
}
    