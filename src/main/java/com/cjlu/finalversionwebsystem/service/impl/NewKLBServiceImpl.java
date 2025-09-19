package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.mapper.NewKLBMapper;
import com.cjlu.finalversionwebsystem.service.Interface.NewKLBInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NewKLBServiceImpl implements NewKLBInterface {
    
    @Autowired
    private NewKLBMapper klbMapper;


    @Override
    public void insertKLB(String KLBName, String KLBCreator, String primaryClassification, String secondaryClassification, String KLBReviseTime, String supportedDataFormats, String KLBSearchStrategy, String description, String creatTime, String KLBStatus) {
        int count = klbMapper.existsByKLBName(KLBName);
        if (count > 0) {
            throw new RuntimeException("KLB with name " + KLBName + " already exists");
        }
        String location = "D:\\基于大模型的企业级知识管理系统\\知识库\\" + KLBName;
        java.io.File directory = new java.io.File(location);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        klbMapper.insertKLB(KLBName, KLBCreator, primaryClassification, secondaryClassification, KLBReviseTime, supportedDataFormats, KLBSearchStrategy, description, creatTime, KLBStatus, location);
    }

    @Override
    public void deleteKLBById(int id) {
        klbMapper.deleteKLBById(id);
    }

    @Override
    public void updateKLBById(int id, String KLBName, String KLBCreator, String primaryClassification, String secondaryClassification, String KLBReviseTime, String supportedDataFormats, String KLBSearchStrategy, String description, String creatTime, String KLBStatus) {
        klbMapper.updateKLBById(id, KLBName, KLBCreator, primaryClassification, secondaryClassification, KLBReviseTime, supportedDataFormats, KLBSearchStrategy, description, creatTime, KLBStatus);
    }

    @Override
    public Map<String, Object> selectKLBById(int id) {
        return klbMapper.selectKLBById(id);
    }

    @Override
    public List<Map<String, Object>> selectAllKLBs() {
        return klbMapper.selectAllKLBs();
    }

    @Override
    public List<Map<String, Object>> selectKLBByKLBCreator(String KLBCreator) {
        return klbMapper.selectKLBByKLBCreator(KLBCreator);
    }

    @Override
    public void updateKLBByKLBName(String KLBName, String KLBCreator, String primaryClassification, String secondaryClassification, String KLBReviseTime, String supportedDataFormats, String KLBSearchStrategy, String description, String creatTime, String KLBStatus) {
        klbMapper.updateKLBByKLBName(KLBName, KLBCreator, primaryClassification, secondaryClassification, KLBReviseTime, supportedDataFormats, KLBSearchStrategy, description, creatTime, KLBStatus);
    }


    public List<String> getFilesByKLBName(String KLBName) {
        Map<String, Object> klb = klbMapper.selectKLBByName(KLBName);
        if (klb == null || !klb.containsKey("location")) {
            throw new RuntimeException("KLB with name " + KLBName + " does not exist or location is not found");
        }
        String location = (String) klb.get("location");
        File directory = new File(location);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new RuntimeException("Directory for KLB with name " + KLBName + " does not exist");
        }
        File[] files = directory.listFiles();
        List<String> fileNames = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                fileNames.add(file.getName());
            }
        }
        return fileNames;
    }

    @Override
    public List<Map<String, Object>> getTopFiveKLBByAccessCount() {
        return klbMapper.selectTopFiveByAccessCount();
    }

}
