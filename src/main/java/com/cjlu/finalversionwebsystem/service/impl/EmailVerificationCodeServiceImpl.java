package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.service.Interface.EmailVerificationCodeServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailVerificationCodeServiceImpl implements EmailVerificationCodeServiceInterface {

    @Autowired
    private JavaMailSender mailSender; // 自动注入邮件发送器

    @Override
    public void sendVerificationCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage(); // 创建一个简单的邮件消息对象
            message.setFrom("q15068673111@sina.cn"); // 设置发件人邮箱地址
            message.setTo(to); // 设置收件人邮箱地址
            message.setSubject("Verification Code"); // 设置邮件主题
            message.setText("您的验证码是：" + code + "\n有效期5分钟，请不要泄露给他人。"); // 设置邮件正文内容
            mailSender.send(message); // 发送邮件
            log.info("邮件发送成功，收件人: {}, 验证码: {}", to, code);
        } catch (Exception e) {
            log.error("邮件发送失败，收件人: {}, 验证码: {}, 错误信息: {}", to, code, e.getMessage(), e);
        }
    }
}

