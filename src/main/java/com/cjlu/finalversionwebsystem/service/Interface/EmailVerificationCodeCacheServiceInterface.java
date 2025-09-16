package com.cjlu.finalversionwebsystem.service.Interface;

public interface EmailVerificationCodeCacheServiceInterface {
    public void saveCode(String email, String code);
    public boolean validateCode(String email, String inputCode);
}

