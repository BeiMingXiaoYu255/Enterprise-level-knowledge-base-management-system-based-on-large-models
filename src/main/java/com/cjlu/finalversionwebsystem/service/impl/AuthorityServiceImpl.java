package com.cjlu.finalversionwebsystem.service.impl;


import com.cjlu.finalversionwebsystem.entity.AuthorityResult;
import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.mapper.AuthorityMapper;
import com.cjlu.finalversionwebsystem.service.Interface.AuthorityServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AuthorityServiceImpl implements AuthorityServiceInterface {

    @Autowired
    private AuthorityMapper authorityMapper;

    public Result showAuthority() {
        try {
            //查询所有的知识库名字
            List<String> klbNameList = authorityMapper.getAllKlbName();
            //将每一条知识库对应的数据全部封装起来
            List<AuthorityResult> authorityList = new ArrayList<>();
            for(String klbName:klbNameList)
            {
                //获取知识库的所有者及其邮箱
                String owner=authorityMapper.getOwner(klbName);
                String owner_email=authorityMapper.getOwner_Email(owner);
                //获取知识库的所有管理人及其邮箱
                List<String> custodians=authorityMapper.getCustodian(klbName);
                List<String> custodians_email=new ArrayList<>();
                for(String custodian:custodians){
                    custodians_email.add(authorityMapper.getOwner_Email(custodian));
                }
                //将一个知识库的所有信息先封装到一个实体类中，再加到这个列表中
                AuthorityResult authorityResult=new AuthorityResult(klbName,owner,owner_email,custodians,custodians_email);
                authorityList.add(authorityResult);
            }
            return Result.success(authorityList);
        } catch (Exception e) {
            log.error("获取知识库权限失败：{}", e.getMessage());
            return Result.error("获取知识库权限失败");
        }
    }

    public Result getAuthority(String klbName) {
         try {
             //对知识库进行模糊查询，先将输入的字符串所对应的所有知识库名字找出来
             List<String> klbNameList = authorityMapper.getKlbName(klbName);
             //将每一条知识库对应的数据全部封装起来
             List<AuthorityResult> authorityList = new ArrayList<>();
             for(String KLBName:klbNameList)
             {
                 //获取知识库的所有者及其邮箱
                 String owner=authorityMapper.getOwner(KLBName);
                 String owner_email=authorityMapper.getOwner_Email(owner);
                 //获取知识库的所有管理人及其邮箱
                 List<String> custodians=authorityMapper.getCustodian(KLBName);
                 List<String> custodians_email=new ArrayList<>();
                 for(String custodian:custodians){
                     custodians_email.add(authorityMapper.getOwner_Email(custodian));
                 }
                 //将一个知识库的所有信息先封装到一个实体类中，再加到这个列表中
                 AuthorityResult authorityResult=new AuthorityResult(KLBName,owner,owner_email,custodians,custodians_email);
                 authorityList.add(authorityResult);
             }
             return Result.success(authorityList);
        } catch (Exception e) {
            log.error("查询权限信息失败", e);
            return Result.error("查询权限信息失败", e);
        }
    }


}
