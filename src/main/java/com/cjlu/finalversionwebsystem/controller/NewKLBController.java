package com.cjlu.finalversionwebsystem.controller;

import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.service.Interface.NewKLBInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.cjlu.finalversionwebsystem.utils.CookieService;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("KLB")
@Slf4j
public class NewKLBController {
    @Autowired
     private NewKLBInterface klbservice;
    
    @PostMapping("creat")
    public Result createKLB(@RequestBody Map<String, Object> request) {
        try {
            log.info("Received KLB creation request: {}", request);
            String KLBName = (String) request.get("KLBName");
            String KLBCreator = (String) request.get("KLBCreator");
            String primaryClassification = (String) request.get("primaryClassification");
            String secondaryClassification = (String) request.get("secondaryClassification");
            String KLBReviseTime = java.time.LocalDateTime.now().toString();
            String supportedDataFormats = (String) request.get("supportedDataFormats");
            String KLBSearchStrategy = (String) request.get("KLBSearchStrategy");
            String description = (String) request.get("description");
            String creatTime = java.time.LocalDateTime.now().toString();
            String KLBStatus = (String) request.get("KLBStatus");

            klbservice.insertKLB(KLBName, KLBCreator, primaryClassification, secondaryClassification, KLBReviseTime, supportedDataFormats, KLBSearchStrategy, description, creatTime, KLBStatus);
            log.info("KLB created successfully: {}", KLBName);
            return Result.success("知识库创建成功");
        } catch (Exception e) {
            log.error("Error during KLB creation: ", e);
            return Result.error("知识库创建失败: " + e.getMessage());
        }
    }
    
    
    public String getUserNameByCookie(HttpServletRequest httpServletRequest){
        return CookieService.getUsernameFromCookie(httpServletRequest);
    }

    @PostMapping("selectKLBByCreatorName")
    public Result selectKLBByCreatorName(HttpServletRequest httpServletRequest) {
        String creatorName = getUserNameByCookie(httpServletRequest);
        if (creatorName == null) {
            return Result.error("未找到创建者名称");
        }
        List<Map<String, Object>> klbList = klbservice.selectKLBByKLBCreator(creatorName);
        return Result.success(klbList);
    }

    @PostMapping("selectAllKLB")
    public Result selectAllKLB() {
        try {
            List<Map<String, Object>> klbList = klbservice.selectAllKLBs();
            return Result.success(klbList);
        } catch (Exception e) {
            log.error("Error during KLB selection: ", e);
            return Result.error("查询所有知识库失败: " + e.getMessage());
        }
    }

    @PostMapping("updateKLB")
    public Result updateKLB(@RequestBody Map<String, Object> request) {
        try {
            log.info("Received KLB update request: {}", request);
            int id = (int) request.get("id");
            String KLBName = (String) request.get("KLBName");
            String KLBCreator = (String) request.get("KLBCreator");
            String primaryClassification = (String) request.get("primaryClassification");
            String secondaryClassification = (String) request.get("secondaryClassification");
            String KLBReviseTime = java.time.LocalDateTime.now().toString();
            String supportedDataFormats = (String) request.get("supportedDataFormats");
            String KLBSearchStrategy = (String) request.get("KLBSearchStrategy");
            String description = (String) request.get("description");
            String creatTime = (String) request.get("creatTime");
            String KLBStatus = (String) request.get("KLBStatus");

            klbservice.updateKLBById(id, KLBName, KLBCreator, primaryClassification, secondaryClassification, KLBReviseTime, supportedDataFormats, KLBSearchStrategy, description, creatTime, KLBStatus);
            log.info("KLB updated successfully: {}", KLBName);
            return Result.success("知识库更新成功");
        } catch (Exception e) {
            log.error("Error during KLB update: ", e);
            return Result.error("更新知识库信息失败" + e.getMessage());
        }
    }

    @PostMapping("open")
    public Result openKLB(@RequestBody Map<String, Object> request) {
        try {
            String KLBName = (String) request.get("KLBName");
            if (KLBName == null || KLBName.isEmpty()) {
                return Result.error("知识库名称不能为空");
            }

            File folder = new File("path/to/your/folder/" + KLBName);
            if (!folder.exists() || !folder.isDirectory()) {
                return Result.error("指定的知识库文件夹不存在");
            }

            File[] files = folder.listFiles();
            if (files == null) {
                return Result.success(new ArrayList<>());
            }

            List<String> filenames = new ArrayList<>();
            for (File file : files) {
                if (file.isFile()) {
                    filenames.add(file.getName());
                }
            }

            return Result.success(filenames);
        } catch (Exception e) {
            log.error("Error during opening KLB: ", e);
            return Result.error("打开知识库失败: " + e.getMessage());
        }
    }

    @PostMapping("access")
    public Result getKLBByAccessCount(){
        return Result.success(klbservice.getTopFiveKLBByAccessCount());
    }

    @PostMapping("get_KLB_by_classification")
    public Result getKLBByPrimaryClassification(@RequestBody Map<String,Object> request){
        return Result.success(klbservice.getTopTenKLBByPrimaryClassification((String) request.get("primary_classification")));
    }

}
