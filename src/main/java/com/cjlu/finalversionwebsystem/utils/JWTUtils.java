package com.cjlu.finalversionwebsystem.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JWTUtils {
    // 密钥
    private static final String SECRET_KEY = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    // 过期时间：2小时
    private static final long EXPIRATION_TIME = 7_200_000;
    // JWT发行人
    private static final String ISSUER = "BeiMingXiaoYu";

    /*
    *
     * 生成JWT Token
     * @param username 用户唯一标识
     * @param claims 自定义声明(可选)
     * @return JWT字符串
    */
    public static String createToken(String username, Map<String, Object> claims) {
        // 如果没有自定义声明，创建空Map
        if (claims == null) {
            claims = new HashMap<>();
        }

        // 创建JWT构建器
        return Jwts.builder()
                // 设置自定义声明
                .setClaims(claims)
                // 设置标准声明：用户名
                .setSubject(username)
                // 设置标准声明：发行人
                .setIssuer(ISSUER)
                // 设置标准声明：签发时间
                .setIssuedAt(new Date())
                // 设置标准声明：过期时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                // 设置签名
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /*
    *
     * 解析JWT Token并获取Claims
     * @param token JWT字符串
     * @return 包含声明的Claims对象
     * @throws JwtException 如果token无效或已过期
     */
    public static Claims parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /*
    *
     * 验证JWT Token是否有效
     * @param token JWT字符串
     * @return 如果有效返回true，否则返回false
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /*
    *
     * 从JWT Token中获取用户名
     * @param token JWT字符串
     * @return 用户名，如果token无效则返回null
     */
    public static String getUsernameFromToken(String token) {
        try {
            return parseToken(token).getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    /*
    *
     * 生成签名密钥
     * @return 用于签名的SecretKeySpec对象
     */
    private static SecretKeySpec getSigningKey() {
        // 确保密钥是Base64编码的，如果不是需要先进行编码
        byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }

    public static long getTokenExpiration(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().getTime();
        } catch (JwtException e) {
            return -1;
        }
    }
}
    

