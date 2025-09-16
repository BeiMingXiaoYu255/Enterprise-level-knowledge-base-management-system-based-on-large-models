package com.cjlu.finalversionwebsystem.controller;

import com.cjlu.finalversionwebsystem.entity.Permission;
import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.service.Interface.PermissionServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/permission")
public class PermissionController {
    @Autowired
    private PermissionServiceInterface permissionService;

    /**
     * 更新用户权限
     *
     * @param request 包含用户名、知识库名和权限等级的请求体
     * @return 返回更新操作的结果
     */
    @PostMapping("/updatePermission")
    public Result updatePermission(@RequestBody Map<String, Object> request) {
        Permission permission = new Permission();
        // 设置用户名
        permission.setUserName((String) request.get("userName"));
        // 设置知识库名
        permission.setKLBName((String) request.get("KLBName"));
        // 设置权限等级
        permission.setPermission((Integer) request.get("permission"));
        // 调用服务层方法更新权限
        return permissionService.updatePermission(permission);
    }

    /**
     * 创建新的权限
     *
     * @param request 包含用户名、知识库名和权限等级的请求体
     * @return 返回创建操作的结果
     */
    @PostMapping("/createPermission")
    public Result createPermission(@RequestBody Map<String, Object> request) {
        Permission permission = new Permission();
        // 设置用户名
        permission.setUserName((String) request.get("userName"));
        // 设置知识库名
        permission.setKLBName((String) request.get("KLBName"));
        // 设置权限等级
        permission.setPermission((Integer) request.get("permission"));
        // 调用服务层方法创建权限
        return permissionService.createPermission(permission);
    }

    /**
     * 删除用户的指定知识库的权限
     *
     * @param request 包含用户名和知识库名的请求体
     * @return 返回删除操作的结果
     */
    @PostMapping("/deletePermission")
    public Result deletePermission(@RequestBody Map<String, Object> request) {
        Permission permission = new Permission();
        // 设置用户名
        permission.setUserName((String) request.get("userName"));
        // 设置知识库名
        permission.setKLBName((String) request.get("KLBName"));
        // 调用服务层方法删除指定知识库的权限
        return permissionService.deletePermission(permission);
    }

    /**
     * 删除用户所有知识库的权限
     *
     * @param request 包含用户名的请求体
     * @return 返回删除操作的结果
     */
    @PostMapping("/deleteAllPermission")
    public Result deleteAllPermission(@RequestBody Map<String, Object> request) {
        Permission permission = new Permission();
        // 设置用户名
        permission.setUserName((String) request.get("userName"));
        // 调用服务层方法删除所有知识库的权限
        return permissionService.deleteAllPermission(permission);
    }
}

