package com.cjlu.finalversionwebsystem.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Slf4j
@Service
public class CookieService {

    JWTUtils jwtUtils = new JWTUtils();
    public static final String JWT_COOKIE_NAME = "TheLifeIsGone";

    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        // 打印请求信息用于调试
        log.debug("尝试获取Cookie: {}，请求来源: {}", cookieName, request.getHeader("Origin"));

        // 从Authorization头中获取Cookie值
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7); // 去掉"Bearer "前缀
            return token;
        }

        log.debug("未找到名为 {} 的Cookie", cookieName);
        return null;
    }

    // 添加设置Cookie的工具方法（确保Cookie属性正确）
    public static void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, token);
        cookie.setPath("/"); // 所有路径均可访问
        cookie.setHttpOnly(false); // 允许前端读取（如果需要通过JS操作）
        // 开发环境设置
        cookie.setDomain("localhost"); // 与前端域名一致
        // 生产环境建议设置为具体域名，如：
        // cookie.setDomain(".yourdomain.com");

        cookie.setMaxAge(7200); // 2小时有效期
        cookie.setSecure(false); // 开发环境设为false，生产环境HTTPS下设为true
        response.addCookie(cookie);
        log.debug("已设置JWT Cookie: {}", JWT_COOKIE_NAME);
    }

    public static String getUsernameFromCookie(HttpServletRequest httpServletRequest) {
        String cookieName = "TheLifeIsGone";
        String token = getCookieValue(httpServletRequest,cookieName);
        return JWTUtils.getUsernameFromToken(token);
    }
}
