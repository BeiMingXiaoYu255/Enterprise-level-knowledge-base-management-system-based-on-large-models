//package com.cjlu.finalversionwebsystem.service.impl;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class AIServiceImpl {
//
//    @Value("${openai.api.key}")
//    private String apiKey;
//
//    @Value("${openai.api.model:default-model}")
//    private String model; // 允许设置默认值
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    public String generateResponse(String prompt) {
//        String apiUrl = "https://ai.nengyongai.cn/v1/chat/completions";
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("Authorization", "Bearer " + apiKey);
//
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("model", model);
//
//        Map<String, String> message = new HashMap<>();
//        message.put("role", "user");
//        message.put("content", prompt);
//        requestBody.put("messages", List.of(message));
//        requestBody.put("temperature", 0.7);
//
//        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
//
//        try {
//            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                Map<String, Object> body = response.getBody();
//                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
//                if (choices != null && !choices.isEmpty()) {
//                    Map<String, Object> firstChoice = choices.get(0);
//                    Map<String, String> messageResponse = (Map<String, String>) firstChoice.get("message");
//                    return messageResponse.get("content");
//                }
//            } else {
//                // 处理非200状态码的情况
//                return "Error: " + response.getStatusCode() + " - " + response.getBody().get("error").toString();
//            }
//            return "No response generated";
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Error: " + e.getMessage();
//        }
//    }
//}