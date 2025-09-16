package com.cjlu.finalversionwebsystem.controller;


import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.service.Interface.AuthorityServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
public class AuthorityController {

    @Autowired
    private AuthorityServiceInterface authorityServiceInterface;

    //界面展示功能
    @GetMapping("/auth/show")
    public Result showAuthority() {
        log.info("查询所有权限信息");
        try {
            return authorityServiceInterface.showAuthority();
        } catch (Exception e) {
            log.error("查询所有权限信息失败", e);
            return Result.error("查询所有权限信息失败", e);
        }
    }

    //查找功能，输入知识库名字，必须将完整知识库名字输入
    @PostMapping("/auth/select")
    public Result getAuthority(String klbName) {
        log.info("权限查找功能");
        try{
            return authorityServiceInterface.getAuthority(klbName);
        } catch (Exception e) {
            log.error("查询权限信息失败", e);
            return Result.error("查询权限信息失败", e);
        }
    }
}
