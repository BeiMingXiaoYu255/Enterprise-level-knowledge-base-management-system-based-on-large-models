package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.File.service.FileStorageService;
import com.cjlu.finalversionwebsystem.mapper.ProfilePictureMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class ProfilePictureServiceImpl {

    @Autowired
    ProfilePictureMapper profilePictureMapper;

    @Autowired
    FileStorageService fileStorageService;

}
