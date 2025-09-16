package com.cjlu.finalversionwebsystem.utils;

import java.util.Random;

public class EmailVerificationCodeUtils {
    // 定义一个随机数生成器
    private static final Random random = new Random();

    // 生成指定长度的验证码
    public static String generateCode(int length) {
        // 创建一个字符数组来存储验证码
        char[] code = new char[length];
        // 循环生成每一位验证码
        for (int i = 0; i < length; i++) {
            // 生成一个0到9之间的随机数并转换为字符
            code[i] = (char) ('0' + random.nextInt(10));
        }
        // 将字符数组转换为字符串并返回
        return new String(code);
    }
}