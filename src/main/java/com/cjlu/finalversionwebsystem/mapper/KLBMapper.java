package com.cjlu.finalversionwebsystem.mapper;

import com.cjlu.finalversionwebsystem.entity.KLB;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KLBMapper {
    // 创建KLB表
    @Update("CREATE TABLE IF NOT EXISTS klb (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "KLBName VARCHAR(255) NOT NULL, " +
            "KLBCreator VARCHAR(255) NOT NULL, " +
            "primaryClassification VARCHAR(255), " +
            "secondaryClassification VARCHAR(255), " +
            "KLBReviseTime VARCHAR(255), " +
            "supportedDataFormats TEXT, " +
            "KLBSearchStrategy VARCHAR(255), " +
            "description VARCHAR(255), " +
            "creatTime VARCHAR(255), " +
            "KLBStatus VARCHAR(255), " +
            "location VARCHAR(255)" +
            "accessCount Integer" +
            ")")
    void createKLBTable();

    // 新增知识库记录（不使用ListTypeHandler）
    @Insert("INSERT INTO klb (KLBName, KLBCreator, primaryClassification, secondaryClassification, KLBReviseTime, " +
            // 插入新的KLB记录
            "supportedDataFormats, KLBSearchStrategy, description, creatTime, KLBStatus, location,accessCount) " +
            "VALUES (#{KLBName}, #{KLBCreator}, #{primaryClassification}, #{secondaryClassification}, #{KLBReviseTime}, " +
            // 直接使用转换后的字符串字段，无需typeHandler
            "#{supportedDataFormatsStr}, #{KLBSearchStrategy}, #{description}, #{creatTime}, #{KLBStatus}, #{location},#{accessCount})")
    void creatNewKLB(KLB klb);

    // 根据KLB名称删除KLB记录
    @Delete("DELETE FROM klb WHERE KLBName = #{KLBName}")
    void deleteKLB(@Param("KLBName") String KLBName);

    // 根据创建者查询KLB记录
    @Select("SELECT * FROM klb WHERE KLBCreator = #{KLBCreator}")
    List<KLB> selectKLBByKLBCreator(@Param("KLBCreator") String KLBCreator);

    // 根据关键词查询KLB记录
    @Select("SELECT * FROM klb WHERE KLBName LIKE CONCAT('%', #{keyword}, '%')")
    List<KLB> selectKLBByKeyWordOfKLBName(@Param("keyword") String keyword);

    // 更新KLB记录
    // 更新KLB记录（不使用ListTypeHandler）
    @Update("<script>UPDATE klb SET " +
            "<if test='KLBName != null'>KLBName = #{KLBName}, </if>" +
            "<if test='KLBCreator != null'>KLBCreator = #{KLBCreator}, </if>" +
            "<if test='primaryClassification != null'>primaryClassification = #{primaryClassification}, </if>" +
            "<if test='secondaryClassification != null'>secondaryClassification = #{secondaryClassification}, </if>" +
            "<if test='KLBReviseTime != null'>KLBReviseTime = #{KLBReviseTime}, </if>" +
            // 移除typeHandler，直接使用转换后的字符串参数
            "<if test='supportedDataFormatsStr != null'>supportedDataFormats = #{supportedDataFormatsStr}, </if>" +
            "<if test='KLBSearchStrategy != null'>KLBSearchStrategy = #{KLBSearchStrategy}, </if>" +
            "<if test='description != null'>description = #{description}, </if>" +
            "<if test='creatTime != null'>creatTime = #{creatTime}, </if>" +
            "<if test='KLBStatus != null'>KLBStatus = #{KLBStatus}, </if>" +
            "<if test='location != null'>location = #{location}, </if>" +
            "WHERE KLBName = #{KLBName}</script>")
    void updateKLB(KLB klb);

    // 根据KLB名称查询文件路径
    @Select("SELECT location FROM klb WHERE KLBName = #{KLBName}")
    String selectLocationByKLBName(@Param("KLBName") String KLBName);

    //每次打开klb都需要在klb表中增加访问次数
    @Update("UPDATE klb SET accessCount = accessCount + 1 WHERE KLBName = #{KLBName}")
    void updateAccessCount(@Param("KLBName") String KLBName);

    //通过一级分类查询最高访问次数的5个知识库，且当访问次数相同时，按照知识库名字首字母ASCLL码升序排列
    @Select("SELECT * FROM klb WHERE primaryClassification = #{primaryClassification} ORDER BY accessCount DESC,KLBName ASC LIMIT 5")
    List<KLB> selectKlbByprimaryClassification(String primaryClassification);

    //直接查询最高访问次数的5个知识库
    @Select("SELECT * FROM klb ORDER BY accessCount DESC LIMIT 5")
    List<KLB> selectklb();
}
