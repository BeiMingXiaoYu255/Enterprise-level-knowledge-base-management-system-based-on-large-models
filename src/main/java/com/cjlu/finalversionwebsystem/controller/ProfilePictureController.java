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

    @GetMapping("/url/{username}")
    public ResponseEntity<String> getProfilePictureUrlByUsername(@PathVariable String username) {
        String url = profilePictureService.getProfilePictureUrlByUsername(username);
        return ResponseEntity.ok(url);
    }
}
