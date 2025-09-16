package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.entity.Permission;
import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.mapper.PermissionMapper;
import com.cjlu.finalversionwebsystem.service.Interface.PermissionServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PermissionServiceImpl implements PermissionServiceInterface {
    
    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public Result updatePermission(Permission permission) {
        // 记录日志，开始更新权限
        log.info("开始更新权限: {}", permission);
        try {
            // 调用Mapper方法更新权限
            permissionMapper.updatePermission(permission.getKLBName(), permission.getUserName(), permission.getPermission());
            // 更新成功，返回成功结果
            return Result.success();
        } catch (Exception e) {
            // 捕获异常，记录错误日志
            log.error("更新权限失败: {}", e.getMessage());
            e.printStackTrace();
            // 返回错误结果
            return Result.error("修改权限失败" + e.getMessage());
        }
    }

    @Override
    public Result createPermission(Permission permission) {
        // 记录日志，开始创建权限
        log.info("开始创建权限: {}", permission);
        try {
            // 调用Mapper方法创建权限
            permissionMapper.setPermission(permission);
            // 创建成功，返回成功结果
            return Result.success();
        } catch (Exception e) {
            // 捕获异常，记录错误日志
            log.error("创建权限失败: {}", e.getMessage());
            e.printStackTrace();
            // 返回错误结果
            return Result.error("创建权限失败" + e.getMessage());
        }
    }

    @Override
    public Result deletePermission(Permission permission) {
        // 记录日志，开始删除指定知识库的权限
        log.info("开始删除指定知识库的权限: {}", permission);
        try {
            // 调用Mapper方法删除指定知识库的权限
            permissionMapper.deletePermissionByUserNameAndKLBName(permission.getUserName(), permission.getUserName());
            // 删除成功，返回成功结果
            return Result.success();
        } catch (Exception e) {
            // 捕获异常，记录错误日志
            log.error("删除指定知识库的权限失败: {}", e.getMessage());
            e.printStackTrace();
            // 返回错误结果
            return Result.error("删除权限失败" + e.getMessage());
        }
    }

    @Override
    public Result deleteAllPermission(Permission permission) {
        // 记录日志，开始删除用户所有知识库的权限
        log.info("开始删除用户所有知识库的权限: {}", permission);
        try {
            // 调用Mapper方法删除用户所有知识库的权限
            permissionMapper.deletePermissionByUserName(permission.getUserName());
            // 删除成功，返回成功结果
            return Result.success();
        } catch (Exception e) {
            // 捕获异常，记录错误日志
            log.error("删除用户所有mcxz知识库的权限失败: {}", e.getMessage());
            e.printStackTrace();
            // 返回错误结果
            return Result.error("删除权限失败" + e.getMessage());
        }
    }
}
