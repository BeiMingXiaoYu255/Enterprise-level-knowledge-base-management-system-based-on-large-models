package com.cjlu.finalversionwebsystem.mapper;

import com.cjlu.finalversionwebsystem.entity.Permission;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PermissionMapper {
    //创建权限表
    @Update("CREATE TABLE IF NOT EXISTS permission (" +
            "id INT PRIMARY KEY AUTO_INCREMENT, " +
            "UserName VARCHAR(255) NOT NULL, " +
            "KLBName VARCHAR(255) NOT NULL, " +
            "permission Int" +
            ")")
    void creatPermissionTable();

    @Select("SELECT * FROM permission WHERE UserName = #{userName}")
    List<Permission> selectPermissionTableByUserName(@Param("userName") String userName);
    
    @Select("SELECT * FROM permission WHERE KLBName = #{KLBName}")
    List<Permission> selectPermissionTableByKLBNAme(@Param("KLBName") String KLBNamne);

    @Select("SELECT * FROM permission WHERE KLBName LIKE CONCAT('%', #{keyword}, '%')")
    List<Permission> selectPermissionTableByKeyKLBName(@Param("keyword") String keyword);

    @Insert("INSERT INTO permission (UserName, KLBName, permission) " +
            "VALUES (#{UserName}, #{KLBName}, #{permission})")
    void setPermission(Permission permission);

    @Delete("DELETE FROM permission WHERE UserName = #{userName}")
    void deletePermissionByUserName(@Param("UserName") String userName);

    @Delete("DELETE FROM permission WHERE UserName = #{userName} AND KLBName = #{KLBName}")
    void deletePermissionByUserNameAndKLBName(@Param("UserName") String userName,@Param("KLBName") String KLBNaem);

    @Update("UPDATE permission SET permission = #{newPermission} WHERE KLBName = #{KLBName} AND UserName = #{UserName}")
    void updatePermission(@Param("KLBName") String KLBName, @Param("UserName") String UserName, @Param("newPermission") Integer newPermission);

}
