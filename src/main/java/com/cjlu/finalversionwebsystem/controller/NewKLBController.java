package com.cjlu.finalversionwebsystem.controller;

import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.service.Interface.NewKLBInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
            String location = (String) request.get("location");

            klbservice.insertKLB(KLBName, KLBCreator, primaryClassification, secondaryClassification, KLBReviseTime, supportedDataFormats, KLBSearchStrategy, description, creatTime, KLBStatus, location);
            log.info("KLB created successfully: {}", KLBName);
            return Result.success("知识库创建成功");
        } catch (Exception e) {
            log.error("Error during KLB creation: ", e);
            return Result.error("知识库创建失败: " + e.getMessage());
        }
    }
}
