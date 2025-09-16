package com.cjlu.finalversionwebsystem.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;


public class CookieService {

    JWTUtils jwtUtils = new JWTUtils();

    /**
     * 从 HttpServletRequest 中获取指定域名的 Cookie 值。
     *
     * @param request    HttpServletRequest 对象
     * @param cookieName Cookie 的名称
     * @return 指定名称和域名的 Cookie 值，如果不存在则返回 null
     */
    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static String getUsernameFromCookie(HttpServletRequest httpServletRequest, String cookieName) {
        cookieName = "TheLifeIsGone";
        String token = getCookieValue(httpServletRequest,cookieName);
        return JWTUtils.getUsernameFromToken(token);
    }
}
