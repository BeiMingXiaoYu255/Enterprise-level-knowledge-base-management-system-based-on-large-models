package com.cjlu.finalversionwebsystem.utils;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 文件加密解密工具类
 * 使用AES算法进行文件加密和解密
 */
@Component
public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    // 固定密钥，实际项目中应该从配置文件或环境变量中获取
    private static final String SECRET_KEY = "MySecretKey12345"; // 16字节密钥
    
    /**
     * 生成AES密钥
     * @return Base64编码的密钥字符串
     * @throws Exception 异常
     */
    public static String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
    
    /**
     * 加密数据
     * @param data 待加密的数据
     * @return 加密后的数据
     * @throws Exception 异常
     */
    public byte[] encrypt(byte[] data) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        return cipher.doFinal(data);
    }
    
    /**
     * 解密数据
     * @param encryptedData 加密的数据
     * @return 解密后的数据
     * @throws Exception 异常
     */
    public byte[] decrypt(byte[] encryptedData) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        return cipher.doFinal(encryptedData);
    }
    
    /**
     * 使用指定密钥加密数据
     * @param data 待加密的数据
     * @param key Base64编码的密钥
     * @return 加密后的数据
     * @throws Exception 异常
     */
    public byte[] encryptWithKey(byte[] data, String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        return cipher.doFinal(data);
    }
    
    /**
     * 使用指定密钥解密数据
     * @param encryptedData 加密的数据
     * @param key Base64编码的密钥
     * @return 解密后的数据
     * @throws Exception 异常
     */
    public byte[] decryptWithKey(byte[] encryptedData, String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        return cipher.doFinal(encryptedData);
    }
}
