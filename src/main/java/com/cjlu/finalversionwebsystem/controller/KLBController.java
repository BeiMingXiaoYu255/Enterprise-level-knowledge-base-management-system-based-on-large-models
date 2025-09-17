package com.cjlu.finalversionwebsystem.controller;

import com.cjlu.finalversionwebsystem.entity.KLB;
import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.service.Interface.KLBServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.cjlu.finalversionwebsystem.utils.CookieService;
import com.cjlu.finalversionwebsystem.utils.JWTUtils;
import com.cjlu.finalversionwebsystem.utils.CookieService;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/KLBS")
@Slf4j
public class KLBController {
    
    @Autowired
    private KLBServiceInterface klbService;

    //创建新知识库
    @PostMapping("/creatKLB")
    public Result creatNewKLB(@RequestBody Map<String, Object> request, HttpServletRequest httpServletRequest) {
        try {
            KLB klb = new KLB(); // 创建一个新的知识库对象

            klb.setKLBName((String) request.get("KLBName")); // 设置知识库名称
            klb.setKLBCreator(getUserName(httpServletRequest)); // 设置创建者
            klb.setDescription((String) request.get("KLBDescription")); // 设置描述
            klb.setKLBStatus((String) request.get("KLBStatus")); // 设置状态
            klb.setKLBSearchStrategy((String) request.get("KLBSearchStrategy")); // 设置搜索策略
            long currentTime = System.currentTimeMillis(); // 获取当前时间
            klb.setKLBReviseTime(String.valueOf(currentTime)); // 设置最后修订时间为当前时间
            klb.setCreatTime(String.valueOf(currentTime)); // 设置创建时间为当前时间
            klb.setSupportedDataFormats(Collections.singletonList((String) request.get("supportedDataFormats"))); // 设置支持的数据格式
            klb.setPrimaryClassification((String) request.get("primaryClassification")); // 设置一级分类
            klb.setSecondaryClassification((String) request.get("secondaryClassification")); // 设置二级分类

            log.info("Creating new knowledge library: {}", klb); // 记录日志：正在创建新的知识库
            log.info("Knowledge library created successfully: {}", klb); // 记录日志：知识库创建成功
            return klbService.creatNewKLB(klb); // 返回成功结果
        } catch (Exception e) {
            log.error("Failed to create knowledge library: {}", e.getMessage(), e); // 记录错误日志：知识库创建失败
            return Result.error("知识库创建失败: " + e.getMessage()); // 返回错误结果
        }
    }


    private String getUserName(HttpServletRequest httpServletRequest) {
        // 从请求中获取用户名，这里假设有一个方法可以从请求中提取用户名
        // 请根据实际情况实现
        return CookieService.getUsernameFromCookie(httpServletRequest);
    }
    
    


    //删除知识库
    @PostMapping("/deleteKLB")
    public Result deleteKLB(@RequestBody Map<String, Object> request) {
        try {
            KLB klb = new KLB();
            klb.setKLBName((String) request.get("KLBName")); // 设置知识库名称
            log.info("Deleting knowledge library: {}", klb); // 记录日志：正在删除知识库
            log.info("Knowledge library deleted successfully: {}", klb); // 记录日志：知识库删除成功
            return klbService.deleteKLB(klb); // 返回成功结果
        } catch (Exception e) {
            log.error("Failed to delete knowledge library: {}", e.getMessage(), e); // 记录错误日志：知识库删除失败
            return Result.error("知识库删除失败: " + e.getMessage()); // 返回错误结果
        }
    }

    //更新知识库
    @PostMapping("/updateKLB")
    public Result updateKLB(@RequestBody Map<String,Object> request){
        KLB klb = new KLB(); // 创建一个新的知识库对象

        klb.setKLBName((String) request.get("KLBName")); // 设置知识库名称
        klb.setKLBCreator((String) request.get("KLBCreator")); // 设置创建者
        klb.setDescription((String) request.get("KLBDescription")); // 设置描述
        klb.setKLBStatus((String) request.get("KLBStatus")); // 设置状态
        klb.setKLBSearchStrategy((String) request.get("KLBSearchStrategy")); // 设置搜索策略
        long currentTime = System.currentTimeMillis(); // 获取当前时间
        klb.setKLBReviseTime(String.valueOf(currentTime)); // 设置最后修订时间为当前时间
        klb.setSupportedDataFormats(Collections.singletonList((String) request.get("supportedDataFormats"))); // 设置支持的数据格式
        klb.setPrimaryClassification((String) request.get("primaryClassification")); // 设置一级分类
        klb.setSecondaryClassification((String) request.get("secondaryClassification")); // 设置二级分类
        return klbService.updateKLB(klb);
    }

    //通过知识库名称查询知识库
    @PostMapping("/selectKLBByKLBName")
    public Result selectKLBByKLBName(@RequestBody Map<String, Object> request) { // 通过知识库名称查询知识库
        KLB klb = new KLB(); // 创建一个新的知识库对象
        klb.setKLBName((String) request.get("KLBName")); // 设置知识库名称
        return klbService.selectKLBByKLBName(klb); // 调用服务层方法查询知识库
    }

    //通过知识库名字打开知识库
    @PostMapping("/openKLBByKLBName")
    public Result openKLBByKLBName(@RequestBody Map<String, Object> request) { // 打开知识库
        KLB klb = new KLB(); // 创建一个新的知识库对象
        klb.setKLBName((String) request.get("KLBName")); // 设置知识库名称
        return klbService.openKLBByKLBName(klb); // 调用服务层方法打开知识库
    }

    //上传文件到指定知识库
    @PostMapping("/uploadFilesByKLBName")
    public Result uploadFilesByKLBName(@RequestBody Map<String, Object> request) { // 上传文件
        KLB klb = new KLB(); // 创建一个新的知识库对象
        String fileName = (String) request.get("fileName"); // 获取文件名
        MultipartFile file = (MultipartFile) request.get("file"); // 获取文件
        klb.setKLBName((String) request.get("KLBName")); // 设置知识库名称
        return klbService.uploadFilesByKLBName(klb, fileName, file); // 调用服务层方法上传文件
    }

    //通过知识库名字和文件名打开文件
    @PostMapping("/openFilesByKLBNameAndFileName")
    public Result openFilesByKLBNameAndFileName(@RequestBody Map<String, Object> request) { // 打开文件
        KLB klb = new KLB(); // 创建一个新的知识库对象
        klb.setKLBName((String) request.get("KLBName")); // 设置知识库名称
        String fileName = (String) request.get("fileName"); // 获取文件名
        return klbService.openFilesByKLBNameAndFileName(klb, fileName); // 调用服务层方法打开文件
    }

    //通过创建者查询知识库
    @PostMapping("/selectKLBByKLBCreator")
    public Result selectKLBByKLBCreator( HttpServletRequest httpServletRequest) {
        try {
            KLB klb = new KLB(); // 创建一个新的知识库对象
            //klb.setKLBCreator(getUserName(httpServletRequest)); // 设置创建者
            klb.setKLBCreator("BeiMingXiaoYu");
            log.info("Selecting knowledge libraries by creator: {}", klb); // 记录日志：正在通过创建者查询知识库
            return klbService.selectKLBByKLBCreator(klb); // 调用服务层方法查询知识库
        } catch (Exception e) {
            log.error("Failed to select knowledge libraries by creator: {}", e.getMessage(), e); // 记录错误日志：通过创建者查询知识库失败
            return Result.error("通过创建者查询知识库失败: " + e.getMessage()); // 返回错误结果
        }
    }

    //通过一级分类查询最高访问次数的5个知识库
    @PostMapping("/selectKlbByprimaryClassification")
    public Result selectKlbByprimaryClassification(@RequestBody Map<String, Object> request) { // 通过一级分类查询知识库
        String primaryClassification=(String) request.get("primaryClassification"); // 获取一级分类
        return klbService.selectKlbByprimaryClassification(primaryClassification); // 调用服务层方法查询知识库
    }
    //直接查询最高访问次数的5个知识库
    @GetMapping("/selectKlb")
    public Result selectklb() {
        return klbService.selectklb();
    }
}
