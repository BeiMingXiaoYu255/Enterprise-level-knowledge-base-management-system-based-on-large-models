package com.cjlu.finalversionwebsystem.service.Interface;

import com.cjlu.finalversionwebsystem.entity.Permission;
import com.cjlu.finalversionwebsystem.entity.Result;

public interface PermissionServiceInterface {
    //根据用户名修和知识库名改用户权限
    Result updatePermission(Permission permission);
    //给予新的权限
    Result createPermission(Permission permission);
    //删除用户的指定知识库的权限
    Result deletePermission(Permission permission);
    //删除用户所有知识库的权限
    Result deleteAllPermission(Permission permission);
}
