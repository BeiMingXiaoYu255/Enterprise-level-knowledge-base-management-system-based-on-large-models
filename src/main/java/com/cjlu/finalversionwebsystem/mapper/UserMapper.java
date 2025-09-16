package com.cjlu.finalversionwebsystem.mapper;

import com.cjlu.finalversionwebsystem.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    //通用操作
    //建立user表
    @Insert("CREATE TABLE IF NOT EXISTS users ("
            + "id INT AUTO_INCREMENT PRIMARY KEY, "
            + "userName VARCHAR(255) NOT NULL, "
            + "passWord VARCHAR(255) NOT NULL, "
            + "email VARCHAR(255) NOT NULL, "
            + "personalSignature VARCHAR(255), "
            + "phone VARCHAR(255), "
            + "sex VARCHAR(255), "
            + "profilePicture VARCHAR(255)"
            + ")")
    void createUserTable();
    
    //查寻用户的个人资料
    @Select("SELECT * FROM users WHERE userName = #{userName}")
    User findUserByUserName(@Param("userName") String userName);


    //向user表里插入数据，用于注册和修改个人资料
    @Insert("INSERT INTO users (userName, passWord, email, personalSignature, phone, sex, profilePicture) " +
            "VALUES (#{userName}, #{passWord}, #{email}, #{personalSignature, jdbcType=VARCHAR}, #{phone, jdbcType=VARCHAR}, #{sex, jdbcType=VARCHAR}, #{profilePicture, jdbcType=VARCHAR})")
    void insertUser(User user);

    //检查用户名是否已存在
    @Select("SELECT EXISTS(SELECT 1 FROM users WHERE userName = #{userName})")
    boolean checkIfUsernameExists(@Param("userName") String userName);

    //登录使用的方法
    //从数据库中提取密码
    @Select("SELECT passWord FROM users WHERE userName = #{userName}")
    String findPasswordByUsername(@Param("userName") String userName);

    //找回密码使用的方法
    //重新设置密码
    @Update("UPDATE users SET passWord = #{newPassword} WHERE userName = #{userName}")
    void updatePasswordByUsername(@Param("userName") String userName, @Param("newPassword") String newPassword);


    @Select("SELECT * FROM users WHERE email = #{email}")
    User findUserByEmail(@Param("email") String email);
}
