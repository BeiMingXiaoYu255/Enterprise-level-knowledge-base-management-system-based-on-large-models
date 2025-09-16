package com.cjlu.finalversionwebsystem.service.Interface;

import com.cjlu.finalversionwebsystem.entity.KLB;
import com.cjlu.finalversionwebsystem.entity.Result;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface KLBServiceInterface {
    // 创建新的知识库
    Result creatNewKLB(KLB klb);

    // 删除知识库
    Result deleteKLB(KLB klb);

    // 更新知识库信息
    Result updateKLB(KLB klb);

    // 根据创建者查询知识库
    Result selectKLBByKLBCreator(KLB klb);

    // 根据知识库名称查询知识库
    Result selectKLBByKLBName(KLB klb);

    //打开知识库
    Result openKLBByKLBName(KLB klb);

    //保存修改后的知识库
    Result saveTheModifiedKLB(KLB klb);

    //上传文件的方法
    Result uploadFilesByKLBName(KLB klb, String fileName, MultipartFile file);

    //打开文件的方法
    Result openFilesByKLBNameAndFileName(KLB klb, String fileName);

    //通过一级分类查询最高访问次数的5个知识库
    Result selectKlbByprimaryClassification(String primaryClassification);

    //直接查询最高访问次数的5个知识库
    Result selectklb();
}
