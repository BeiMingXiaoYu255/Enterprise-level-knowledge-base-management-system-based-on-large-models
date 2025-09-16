package com.cjlu.finalversionwebsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User
{
    //用户唯一标识id
    private Integer id;
    //用户名
    private  String userName;
    //密码
    private String passWord;
    //邮箱
    private  String email;
    //个性签名
    private String personalSignature;
    //电话号码
    private String phone;
    //性别
    private String sex;
    //头像（以路径形式记录）
    private String profilePicture;
}
