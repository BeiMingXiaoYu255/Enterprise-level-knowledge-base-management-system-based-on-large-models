package com.cjlu.finalversionwebsystem.utils;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtils {

    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // 加密密码
    public static String encryptPassword(String password) {
        try {
            SecureRandom random = SecureRandom.getInstanceStrong();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);

            byte[] hash = factory.generateSecret(spec).getEncoded();

            // 将盐和哈希值组合并编码为Base64
            byte[] hashWithSalt = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, hashWithSalt, 0, salt.length);
            System.arraycopy(hash, 0, hashWithSalt, salt.length, hash.length);

            return Base64.getEncoder().encodeToString(hashWithSalt);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    // 验证密码
    public static boolean verifyPassword(String password, String encryptedPassword) {
        try {
            // 解码Base64字符串
            byte[] hashWithSalt = Base64.getDecoder().decode(encryptedPassword);

            // 提取盐
            byte[] salt = new byte[16];
            System.arraycopy(hashWithSalt, 0, salt, 0, 16);

            // 提取哈希值
            byte[] hash = new byte[hashWithSalt.length - 16];
            System.arraycopy(hashWithSalt, 16, hash, 0, hash.length);

            // 使用相同的盐、迭代次数和密钥长度生成新的哈希值
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] testHash = factory.generateSecret(spec).getEncoded();

            // 比较两个哈希值
            return slowEquals(hash, testHash);
        } catch (Exception e) {
            throw new RuntimeException("密码验证失败", e);
        }
    }

    // 防止时序攻击的比较方法
    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    // 生成临时密码
    public static String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+";
        StringBuilder tempPassword = new StringBuilder(12);

        for (int i = 0; i < 12; i++) {
            tempPassword.append(chars.charAt(random.nextInt(chars.length())));
        }

        return tempPassword.toString();
    }

    // 生成验证码
    public static String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(6);

        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }

        return code.toString();
    }

    public static boolean isStrongPassword(String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            return false;
        }

        boolean hasUpperCase = !newPassword.equals(newPassword.toLowerCase());
        boolean hasLowerCase = !newPassword.equals(newPassword.toUpperCase());
        boolean hasDigit = newPassword.matches(".*\\d.*");
        boolean hasSpecialChar = newPassword.matches(".*[!@#$%^&*()-_=+].*");

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar;
    }
}