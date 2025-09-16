package com.cjlu.finalversionwebsystem.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorityResult {
    //知识库的名字
    private String KLBName;
    //所有人
    private String Owner;
    //所有人的邮箱
    private String Owner_Email;
    //知识库的维护人
    private List<String> custodians;
    //知识库的维护人邮箱
    private List<String> custodians_email;
}
