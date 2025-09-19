package com.cjlu.finalversionwebsystem.controller;

import com.cjlu.finalversionwebsystem.service.impl.ProfilePictureServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<Map<String, String>> getProfilePictureUrlByUsername(HttpServletRequest httpServletRequest) {
        String url = profilePictureService.getProfilePictureUrlByUsername(httpServletRequest);
        String username = profilePictureService.getUserNameByCookie(httpServletRequest);

        // 修改URL为前端可以直接访问的路径
        String frontendAccessibleUrl = "http://localhost:8080/" + url;

        Map<String, String> response = new HashMap<>();
        response.put("url", frontendAccessibleUrl);
        response.put("username", username);
        return ResponseEntity.ok(response);
    }
}
