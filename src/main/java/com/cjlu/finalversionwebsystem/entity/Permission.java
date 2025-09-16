package com.cjlu.finalversionwebsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Permission {
    //id
    private Integer id;
    //用户名
    private String UserName;
    //知识库名
    private String KLBName;
    //权限等级（可以为空）
    private Integer permission;
}
