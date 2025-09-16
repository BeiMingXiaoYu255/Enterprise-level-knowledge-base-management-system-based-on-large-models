package com.cjlu.finalversionwebsystem.service.Interface;

public interface EmailVerificationCodeServiceInterface {
    void sendVerificationCode(String to,String code);
}
