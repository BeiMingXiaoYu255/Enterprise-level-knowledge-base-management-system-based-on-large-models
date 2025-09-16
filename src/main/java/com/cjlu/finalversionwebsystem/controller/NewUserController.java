package com.cjlu.finalversionwebsystem.controller;

import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.entity.User;
import com.cjlu.finalversionwebsystem.service.Interface.NewUserServiceInterface;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("LoginRegistration_Page")
@Slf4j
public class NewUserController {
    
    @Autowired
    private NewUserServiceInterface newUserServiceInterface;

    @PostMapping("register")
    public Result register(@RequestBody Map<String, Object> requestMap, HttpServletRequest httpServletRequest) {
        try {
            log.info("Received registration request: {}", requestMap);
            String email = (String) requestMap.get("email");
            String username = (String) requestMap.get("username");
            String password = (String) requestMap.get("password");

            log.info("User {} is trying to register", username);
            User user = new User(null, username, password, email, null, null, null, null);
            log.debug("User object created: {}", user);
            return newUserServiceInterface.register(user);
        } catch (Exception e) {
            log.error("Error during registration: ", e);
            return Result.error("注册失败，请稍后重试");
        }
    }

    @PostMapping("login")
    public Result login(@RequestBody Map<String,Object> requestMap,HttpServletRequest request){
        try {
            String username = (String) requestMap.get("username");
            String password = (String) requestMap.get("password");

            log.info("User {} is trying to log in", username);
            return newUserServiceInterface.login(username, password);
        } catch (Exception e) {
            log.error("Error during login: ", e);
            return Result.error("登录失败");
        }
    }
}
