package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.File.service.FileStorageService;
import com.cjlu.finalversionwebsystem.entity.ProfilePicture;
import com.cjlu.finalversionwebsystem.mapper.ProfilePictureMapper;
import com.cjlu.finalversionwebsystem.utils.CookieService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;

@Service
@Slf4j
public class ProfilePictureServiceImpl {

    @Autowired
    ProfilePictureMapper profilePictureMapper;

    @Autowired
    FileStorageService fileStorageService;



    public String getUserNameByCookie(HttpServletRequest httpServletRequest){
        return CookieService.getUsernameFromCookie(httpServletRequest);
    }

    public void saveUserProfilePicture(MultipartFile file, HttpServletRequest httpServletRequest) {
        try {
            String uploadDir = "D:\\基于大模型的企业级知识管理系统\\头像";
            String fileName = fileStorageService.storeFileToLocationWithoutMd(file, uploadDir);
            String filePath = uploadDir + "\\" + fileName;
            String username = getUserNameByCookie(httpServletRequest);

            // 删除旧的记录和文件
            ProfilePicture oldProfilePicture = profilePictureMapper.selectProfilePictureByUserName(username);
            if (oldProfilePicture != null) {
                String oldFilePath = oldProfilePicture.getProfile_picture_location();
                File oldFile = new File(oldFilePath);
                if (oldFile.exists()) {
                    oldFile.delete();
                }
                profilePictureMapper.deleteProfilePictureByUserName(username);
            }

            profilePictureMapper.insertProfilePicture(filePath, username);
        } catch (Exception e) {
            log.error("Error saving user profile picture: {}", e.getMessage());
        }
    }

    public String getProfilePictureUrlByUsername(String username) {
        return profilePictureMapper.selectProfilePictureByUserName(username).getProfile_picture_location();
    }
}
