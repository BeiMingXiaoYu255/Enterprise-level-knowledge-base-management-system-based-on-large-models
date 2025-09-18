package com.cjlu.finalversionwebsystem.controller;

import com.cjlu.finalversionwebsystem.service.impl.ProfilePictureServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("ProfilePicture")
public class ProfilePictureController {

    @Autowired
    ProfilePictureServiceImpl profilePictureService;

    @PostMapping("/save")
    public ResponseEntity<String> saveUserProfilePicture(@RequestParam("file") MultipartFile file, HttpServletRequest httpServletRequest) {
        profilePictureService.saveUserProfilePicture(file, httpServletRequest);
        return ResponseEntity.ok("Profile picture saved successfully");
    }

    @GetMapping("/url")
    public ResponseEntity<String> getProfilePictureUrlByUsername(HttpServletRequest httpServletRequest) {
        String url = profilePictureService.getProfilePictureUrlByUsername(httpServletRequest);
        return ResponseEntity.ok(url);
    }
}
