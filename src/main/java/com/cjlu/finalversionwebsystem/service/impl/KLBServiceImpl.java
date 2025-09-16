package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.entity.KLB;
import com.cjlu.finalversionwebsystem.entity.Permission;
import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.mapper.KLBMapper;
import com.cjlu.finalversionwebsystem.mapper.PermissionMapper;
import com.cjlu.finalversionwebsystem.service.Interface.KLBServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Slf4j
@Service
public class KLBServiceImpl implements KLBServiceInterface {

    @Autowired
    private KLBMapper klbMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    // 从配置文件读取存储路径，避免硬编码
    @Value("${klb.storage.path:D:/KLBs}")
    private String storagePath;

    @Override
    public Result creatNewKLB(KLB klb) {
        try {
            // 构建知识库文件夹路径，使用配置的存储路径
            String folderPath = storagePath + File.separator + klb.getKLBName();
            File folder = new File(folderPath);

            // 创建文件夹（包括父目录）
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    throw new RuntimeException("无法创建文件夹: " + folderPath);
                }
            }
            //设置知识库初始打开次数为0
            klb.setAccessCount(0);

            // 设置知识库存储位置
            klb.setLocation(folder.getAbsolutePath());

            // 插入klb表
            klbMapper.creatNewKLB(klb);

            // 创建Permission对象并填充属性，设置默认权限为3
            Permission permission = new Permission();
            permission.setPermission(3);
            permission.setKLBName(klb.getKLBName());
            permission.setUserName(klb.getKLBCreator());

            // 插入permission表
            permissionMapper.setPermission(permission);

            return Result.success(klb);
        } catch (Exception e) {
            log.error("创建新的知识库时发生错误: {}", e.getMessage(), e);
            return Result.error("创建新的知识库时发生错误: " + e.getMessage());
        }
    }

    @Override
    public Result deleteKLB(KLB klb) {
        try {
            // 删除KLB表中的记录
            klbMapper.deleteKLB(klb.getKLBName());

            // 删除权限表中的相关记录
            permissionMapper.deletePermissionByUserName(klb.getKLBName());

            // 删除文件夹
            String folderPath = storagePath + File.separator + klb.getKLBName();
            File folder = new File(folderPath);
            if (folder.exists()) {
                deleteFolderRecursively(folder);
            }

            return Result.success("知识库删除成功");
        } catch (Exception e) {
            log.error("删除知识库时发生错误: {}", e.getMessage(), e);
            return Result.error("删除知识库时发生错误: " + e.getMessage());
        }
    }

    // 递归删除文件夹及其所有内容
    private void deleteFolderRecursively(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolderRecursively(file);
                }
                file.delete();
            }
        }
        folder.delete();
    }

    @Override
    public Result updateKLB(KLB klb) {
        try {
            // 更新KLB表中的记录
            klbMapper.updateKLB(klb);
            return Result.success("知识库更新成功");
        } catch (Exception e) {
            log.error("更新知识库时发生错误: {}", e.getMessage(), e);
            return Result.error("更新知识库时发生错误: " + e.getMessage());
        }
    }

    @Override
    public Result selectKLBByKLBCreator(KLB klb) {
        try {
            List<KLB> klbList = klbMapper.selectKLBByKLBCreator(klb.getKLBCreator());
            if (klbList.isEmpty()) {
                return Result.error("未找到该创建者创建的知识库");
            }
            return Result.success(klbList);
        } catch (Exception e) {
            log.error("根据创建者查询知识库时发生错误: {}", e.getMessage(), e);
            return Result.error("根据创建者查询知识库时发生错误: " + e.getMessage());
        }
    }

    @Override
    public Result selectKLBByKLBName(KLB klb) {
        try {
            // 使用关键字模糊查询知识库
            List<KLB> resultKLB = klbMapper.selectKLBByKeyWordOfKLBName(klb.getKLBName());
            if (resultKLB.isEmpty()) { // 修正：判断列表是否为空而非是否为null
                return Result.error("未找到该名称的知识库");
            }
            return Result.success(resultKLB);
        } catch (Exception e) {
            log.error("根据知识库名称查询时发生错误: {}", e.getMessage(), e);
            return Result.error("根据知识库名称查询时发生错误: " + e.getMessage());
        }
    }

    @Override
    public Result openKLBByKLBName(KLB klb) {
        try {
            String folderPath = klb.getLocation();
            File folder = new File(folderPath);

            // 检查文件夹是否存在且有效
            if (!folder.exists() || !folder.isDirectory()) {
                return Result.error("文件夹不存在或不是一个有效的目录: " + folderPath);
            }

            File[] files = folder.listFiles();
            if (files == null || files.length == 0) {
                return Result.error("文件夹为空: " + folderPath);
            }

            // 构建更清晰的返回结构
            Map<String, Object> resultMap = new HashMap<>();
            List<String> fileNames = new ArrayList<>();
            for (File file : files) {
                fileNames.add(file.getName());
            }

            resultMap.put("fileList", fileNames);
            resultMap.put("firstFile", files[0]);

            //将访问的知识库的访问次数+1
            klbMapper.updateAccessCount(klb.getKLBName());

            return Result.success(resultMap);
        } catch (Exception e) {
            log.error("打开知识库时发生错误: {}", e.getMessage(), e);
            return Result.error("打开知识库时发生错误: " + e.getMessage());
        }
    }

    @Override
    public Result saveTheModifiedKLB(KLB klb) {
        try {
            // 更新KLB表中的记录
            klbMapper.updateKLB(klb);
            return Result.success("知识库更新成功");
        } catch (Exception e) {
            log.error("保存修改后的知识库时发生错误: {}", e.getMessage(), e);
            return Result.error("保存修改后的知识库时发生错误: " + e.getMessage());
        }
    }

    @Override
    public Result uploadFilesByKLBName(KLB klb, String fileName, MultipartFile file) {
        try {
            // 根据KLB对象中的location属性和fileName确定路径
            String folderPath = klb.getLocation();
            File targetFolder = new File(folderPath);

            // 检查目标文件夹是否存在且有效
            if (!targetFolder.exists() || !targetFolder.isDirectory()) {
                return Result.error("文件夹不存在或不是一个有效的目录: " + folderPath);
            }

            // 构建目标文件路径
            File targetFile = new File(targetFolder, fileName);

            // 使用Spring的FileCopyUtils更可靠地保存文件
            try (InputStream in = file.getInputStream();
                 OutputStream out = new FileOutputStream(targetFile)) {
                FileCopyUtils.copy(in, out);
            }

            return Result.success("文件上传成功");
        } catch (Exception e) {
            log.error("上传文件时发生错误: {}", e.getMessage(), e);
            return Result.error("上传文件时发生错误: " + e.getMessage());
        }
    }

    @Override
    public Result openFilesByKLBNameAndFileName(KLB klb, String fileName) {
        try {
            // 根据KLB对象中的location属性和fileName确定路径
            String folderPath = klb.getLocation();
            File targetFolder = new File(folderPath);

            // 检查目标文件夹是否存在且有效
            if (!targetFolder.exists() || !targetFolder.isDirectory()) {
                return Result.error("文件夹不存在或不是一个有效的目录: " + folderPath);
            }

            // 构建目标文件路径
            File targetFile = new File(targetFolder, fileName);

            // 检查目标文件是否存在且有效
            if (!targetFile.exists() || !targetFile.isFile()) {
                return Result.error("文件不存在: " + targetFile.getAbsolutePath());
            }

            //将访问的知识库的访问次数+1
            klbMapper.updateAccessCount(klb.getKLBName());

            return Result.success(targetFile);
        } catch (Exception e) {
            log.error("打开文件时发生错误: {}", e.getMessage(), e);
            return Result.error("打开文件时发生错误: " + e.getMessage());
        }
    }
    //通过一级分类查询最高访问次数的5个知识库
    @Override
    public Result selectKlbByprimaryClassification(String primaryClassification)
    {
        try {
            List<KLB> resultKLB = klbMapper.selectKlbByprimaryClassification(primaryClassification);
            if (resultKLB.isEmpty()) {
                return Result.error("未找到该一级分类的知识库");
            }
            return Result.success(resultKLB);
        } catch (Exception e) {
            log.error("根据一级分类查询知识库时发生错误: {}", e.getMessage(), e);
            return Result.error("根据一级分类查询知识库时发生错误: " + e.getMessage());
        }
    }

    //直接查询最高访问次数的5个知识库
    @Override
    public Result selectklb() {
        try {
            List<KLB> resultKLB = klbMapper.selectklb();
            if (resultKLB.isEmpty()) {
                return Result.error("未找到该一级分类的知识库");
            }
            return Result.success(resultKLB);
        } catch (Exception e) {
            log.error("根据一级分类查询知识库时发生错误: {}", e.getMessage(), e);
            return Result.error("根据一级分类查询知识库时发生错误: {}"+ e.getMessage());
        }
    }
}