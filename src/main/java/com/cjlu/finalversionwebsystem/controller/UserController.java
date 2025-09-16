package com.cjlu.finalversionwebsystem.controller;

import cn.hutool.captcha.*;
import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.entity.User;
import com.cjlu.finalversionwebsystem.service.Interface.UserServiceInterface;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/user")
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserServiceInterface userServiceInterface;

    private static final String SESSION_KEY = "captcha";
    @Autowired

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, Object> requestMap,
                        HttpServletRequest request) {
        try {
            String username = (String) requestMap.get("username");
            String password = (String) requestMap.get("password");
            String captchaCode = (String) requestMap.get("captchaCode");

            log.info("User {} is trying to log in", username);
            return userServiceInterface.login(username, password);
        } catch (Exception e) {
            log.error("Error during login: ", e);
            return Result.error("登录失败，请稍后重试");
        }
    }

    @PostMapping("/register")
    public Result register(@RequestBody Map<String, Object> requestMap,
                           HttpServletRequest request) {
        try {
            log.info("Received registration request: {}", requestMap);
            String email = (String) requestMap.get("email");
            String username = (String) requestMap.get("username");
            String password = (String) requestMap.get("password");
            String confirmPassword = (String) requestMap.get("confirmPassword");
            String captchaCode = (String) requestMap.get("captchaCode");

            // 验证验证码
            String captcha = request.getHeader(SESSION_KEY);
            if (captcha == null || !captchaCode.equalsIgnoreCase(captcha)) {
                log.info("验证码错误");
                return Result.error("验证码错误");
            }

            log.info("User {} is trying to register", username);
            User user = new User(null, username, password, email, null, null, null, null);
            log.debug("User object created: {}", user);
            return userServiceInterface.register(user, confirmPassword);
        } catch (Exception e) {
            log.error("Error during registration: ", e);
            return Result.error("注册失败，请稍后重试");
        }
    }

    @PostMapping("/recover-password")
    public Result recoverPassword(@RequestBody Map<String, String> requestMap,
                                  HttpServletRequest request) {
        try {
            String email = requestMap.get("email");
            String userName = requestMap.get("userName");
            String captchaCode = requestMap.get("captchaCode");

            // 从Session获取验证码
            HttpSession session = request.getSession();
            String sessionCaptchaCode = (String) session.getAttribute(SESSION_KEY);

            // 验证后立即清除
            session.removeAttribute(SESSION_KEY);

            if (sessionCaptchaCode == null || !sessionCaptchaCode.equals(captchaCode)) {
                return Result.error("验证码错误");
            }

            log.info("Recovering password for email: {} and user name: {}", email, userName);
            return userServiceInterface.resetPassword(email, userName);
        } catch (Exception e) {
            log.error("Error during password recovery: ", e);
            return Result.error("找回密码失败，请稍后重试");
        }
    }

    @GetMapping(value = "/captcha", produces = "image/png")
    public void generateCaptcha(HttpServletResponse response) throws Exception {
        log.info("生成验证码");

        // 1. 先创建验证码并获取文本
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(100, 40, 4, 40);
        String code = captcha.getCode();

        // 2. 先设置响应头（在响应体输出之前）
        response.setHeader(SESSION_KEY, code);
        response.setHeader("Access-Control-Expose-Headers", SESSION_KEY); // 允许前端读取
        response.setContentType("image/png"); // 明确设置图片类型（避免默认类型错误）

        // 3. 最后输出响应体（验证码图片）
        captcha.write(response.getOutputStream());
        response.getOutputStream().flush(); // 刷新流（可选，容器会自动处理）
    }


}


