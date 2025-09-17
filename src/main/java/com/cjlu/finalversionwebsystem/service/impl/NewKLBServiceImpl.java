package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.mapper.NewKLBMapper;
import com.cjlu.finalversionwebsystem.service.Interface.NewKLBInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NewKLBServiceImpl implements NewKLBInterface {
    
    @Autowired
    private NewKLBMapper klbMapper;
    

    @Override
    public void insertKLB(String KLBName, String KLBCreator, String primaryClassification, String secondaryClassification, String KLBReviseTime, String supportedDataFormats, String KLBSearchStrategy, String description, String creatTime, String KLBStatus, String location) {
        klbMapper.insertKLB(KLBName, KLBCreator, primaryClassification, secondaryClassification, KLBReviseTime, supportedDataFormats, KLBSearchStrategy, description, creatTime, KLBStatus, location);
    }

    @Override
    public void deleteKLBById(int id) {
        klbMapper.deleteKLBById(id);
    }

    @Override
    public void updateKLBById(int id, String KLBName, String KLBCreator, String primaryClassification, String secondaryClassification, String KLBReviseTime, String supportedDataFormats, String KLBSearchStrategy, String description, String creatTime, String KLBStatus, String location, Integer accessCount) {
        klbMapper.updateKLBById(id, KLBName, KLBCreator, primaryClassification, secondaryClassification, KLBReviseTime, supportedDataFormats, KLBSearchStrategy, description, creatTime, KLBStatus, location, accessCount);
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
}
