package com.cjlu.finalversionwebsystem.mapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface NewKLBMapper {

    @Insert("INSERT INTO klb (KLBName, KLBCreator, primaryClassification, secondaryClassification, KLBReviseTime, supportedDataFormats, KLBSearchStrategy, description, creatTime, KLBStatus, location, accessCount) " +
            "VALUES (#{KLBName}, #{KLBCreator}, #{primaryClassification}, #{secondaryClassification}, #{KLBReviseTime}, #{supportedDataFormats}, #{KLBSearchStrategy}, #{description}, #{creatTime}, #{KLBStatus}, #{location}, 0)")
    void insertKLB(@Param("KLBName") String KLBName, @Param("KLBCreator") String KLBCreator, @Param("primaryClassification") String primaryClassification, @Param("secondaryClassification") String secondaryClassification, @Param("KLBReviseTime") String KLBReviseTime, @Param("supportedDataFormats") String supportedDataFormats, @Param("KLBSearchStrategy") String KLBSearchStrategy, @Param("description") String description, @Param("creatTime") String creatTime, @Param("KLBStatus") String KLBStatus, @Param("location") String location);

    @Delete("DELETE FROM klb WHERE id = #{id}")
    void deleteKLBById(@Param("id") int id);

    @Update("<script>UPDATE klb SET " +
            "<if test='KLBName != null'>KLBName = #{KLBName}, </if>" +
            "<if test='KLBCreator != null'>KLBCreator = #{KLBCreator}, </if>" +
            "<if test='primaryClassification != null'>primaryClassification = #{primaryClassification}, </if>" +
            "<if test='secondaryClassification != null'>secondaryClassification = #{secondaryClassification}, </if>" +
            "<if test='KLBReviseTime != null'>KLBReviseTime = #{KLBReviseTime}, </if>" +
            "<if test='supportedDataFormats != null'>supportedDataFormats = #{supportedDataFormats}, </if>" +
            "<if test='KLBSearchStrategy != null'>KLBSearchStrategy = #{KLBSearchStrategy}, </if>" +
            "<if test='description != null'>description = #{description}, </if>" +
            "<if test='creatTime != null'>creatTime = #{creatTime}, </if>" +
            "<if test='KLBStatus != null'>KLBStatus = #{KLBStatus}, </if>" +
            "WHERE id = #{id}</script>")
    void updateKLBById(@Param("id") int id, @Param("KLBName") String KLBName, @Param("KLBCreator") String KLBCreator, @Param("primaryClassification") String primaryClassification, @Param("secondaryClassification") String secondaryClassification, @Param("KLBReviseTime") String KLBReviseTime, @Param("supportedDataFormats") String supportedDataFormats, @Param("KLBSearchStrategy") String KLBSearchStrategy, @Param("description") String description, @Param("creatTime") String creatTime, @Param("KLBStatus") String KLBStatus);

    @Update("<script>UPDATE klb SET " +
            "<if test='KLBName != null'>KLBName = #{KLBName}, </if>" +
            "<if test='KLBCreator != null'>KLBCreator = #{KLBCreator}, </if>" +
            "<if test='primaryClassification != null'>primaryClassification = #{primaryClassification}, </if>" +
            "<if test='secondaryClassification != null'>secondaryClassification = #{secondaryClassification}, </if>" +
            "<if test='KLBReviseTime != null'>KLBReviseTime = #{KLBReviseTime}, </if>" +
            "<if test='supportedDataFormats != null'>supportedDataFormats = #{supportedDataFormats}, </if>" +
            "<if test='KLBSearchStrategy != null'>KLBSearchStrategy = #{KLBSearchStrategy}, </if>" +
            "<if test='description != null'>description = #{description}, </if>" +
            "<if test='creatTime != null'>creatTime = #{creatTime}, </if>" +
            "<if test='KLBStatus != null'>KLBStatus = #{KLBStatus}, </if>" +
            "WHERE KLBName = #{KLBName}</script>")
    void updateKLBByKLBName(@Param("KLBName") String KLBName, @Param("KLBCreator") String KLBCreator, @Param("primaryClassification") String primaryClassification, @Param("secondaryClassification") String secondaryClassification, @Param("KLBReviseTime") String KLBReviseTime, @Param("supportedDataFormats") String supportedDataFormats, @Param("KLBSearchStrategy") String KLBSearchStrategy, @Param("description") String description, @Param("creatTime") String creatTime, @Param("KLBStatus") String KLBStatus);


    @Select("SELECT * FROM klb WHERE id = #{id}")
    Map<String, Object> selectKLBById(@Param("id") int id);

    @Select("SELECT * FROM klb WHERE KLBCreator = #{KLBCreator}")
    List<Map<String, Object>> selectKLBByKLBCreator(@Param("KLBCreator") String KLBCreator);

    @Select("SELECT * FROM klb")
    List<Map<String, Object>> selectAllKLBs();
    
    @Select("SELECT COUNT(*) FROM klb WHERE KLBName = #{KLBName}")
    int existsByKLBName(@Param("KLBName") String KLBName);


}
