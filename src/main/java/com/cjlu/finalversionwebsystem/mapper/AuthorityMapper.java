package com.cjlu.finalversionwebsystem.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AuthorityMapper {
    //通过知识库名字在知识库表中查找知识库创建者
    @Select("SELECT KLBCreator FROM klb WHERE KLBName = #{klbName}")
    String getOwner(String klbName);
    //通过用户名在用户表中查找邮箱
    @Select("SELECT email FROM users WHERE userName= #{userName}")
    String getOwner_Email(String userName);
    //通过知识库名字在权限表中找到对应权限等级等于3的所有人
    @Select("SELECT userName FROM permission WHERE KLBName= #{klbName}")
    List<String> getCustodian(String klbName);
    //查询所有知识库名字
    @Select("SELECT KLBName FROM klb")
    List<String> getAllKlbName();
    //通过输入的名字返回所有的知识库名字
    @Select("SELECT KLBName FROM klb WHERE KLBName LIKE CONCAT('%',#{userName},'%')")
    List<String> getKlbName(String userName);
}
