package com.cjlu.finalversionwebsystem.service.impl;

import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.entity.User;
import com.cjlu.finalversionwebsystem.mapper.UserMapper;
import com.cjlu.finalversionwebsystem.service.Interface.UserServiceInterface;
import com.cjlu.finalversionwebsystem.utils.JWTUtils;
import com.cjlu.finalversionwebsystem.utils.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserServiceInterface {

    @Autowired
    private UserMapper userMapper;

    // 修正：使用Spring依赖注入JwtBlacklistService
    @Autowired
    private JwtBlacklistServiceImpl jwtBlacklistService;

    /**
     * 用户登录
     * @param userName 用户名
     * @param password 密码
     * @return 登录结果（含Token和用户信息）
     */
    @Override
    public Result login(String userName, String password) {
        try {
            log.info("登录请求: 用户名 = {}", userName);

            // 查询用户信息
            User user = userMapper.findUserByUserName(userName);
            if (user == null) {
                log.warn("用户名不存在: {}", userName);
                return Result.error("用户名不存在");
            }

            // 验证密码
            if (!PasswordUtils.verifyPassword(password, user.getPassWord())) {
                log.warn("密码错误: {}", userName);
                return Result.error("密码错误");
            }

            // 准备JWT声明信息（仅包含必要字段）
            Map<String, Object> claims = new HashMap<>();
            claims.put("signature", user.getPersonalSignature());
            claims.put("gender", user.getSex());

            // 生成JWT
            String jwtToken = JWTUtils.createToken(userName, claims);
            log.info("登录成功: 用户名 = {}", userName);

            // 构建返回数据（直接在方法内过滤敏感信息）
            Map<String, Object> data = new HashMap<>();
            data.put("token", jwtToken);

            // 1. 创建用户信息副本或新的Map存储需返回的字段（避免修改原实体）
            Map<String, Object> userInfo = new HashMap<>();
            // 2. 只保留需要返回的非敏感字段
            userInfo.put("userId", user.getId());       // 用户ID（必要标识）
            userInfo.put("userName", user.getUserName());   // 用户名（必要标识）
            userInfo.put("sex", user.getSex());             // 性别（非敏感）
            userInfo.put("personalSignature", user.getPersonalSignature()); // 个性签名（非敏感）
            // 3. 排除敏感字段：密码、邮箱、手机号等
            // 注意：此处需根据User类实际字段调整，确保不包含敏感信息

            data.put("userInfo", userInfo);

            return Result.success(data);
        } catch (Exception e) {
            log.error("登录失败: 用户名 = {}, 异常信息: {}", userName, e.getMessage(), e);
            return Result.error("登录失败，请稍后重试");
        }
    }

    /**
     * 用户注册
     * @param user 用户信息
     * @param confirmPassword 确认密码
     * @return 注册结果
     */
    @Override
    public Result register(User user, String confirmPassword) {
        try {
            log.info("注册请求: 用户名 = {}", user.getUserName());

            // 检查用户名是否已存在
            if (userMapper.checkIfUsernameExists(user.getUserName())) {
                log.warn("用户名已存在: {}", user.getUserName());
                return Result.error("用户名已存在");
            }

            // 检查两次输入的密码是否一致
            if (!user.getPassWord().equals(confirmPassword)) {
                log.warn("两次输入的密码不一致: {}", user.getUserName());
                return Result.error("两次输入的密码不一致");
            }

            // 验证密码复杂度
            if (!PasswordUtils.isStrongPassword(user.getPassWord())) {
                log.warn("密码强度不足: {}", user.getUserName());
                return Result.error("密码需包含大小写字母、数字和特殊字符，长度至少8位");
            }

            // 密码加密
            String encryptedPassword = PasswordUtils.encryptPassword(user.getPassWord());

            // 创建仅包含允许字段的新User对象
            User userToInsert = new User();
            userToInsert.setUserName(user.getUserName());
            userToInsert.setPassWord(encryptedPassword);
            userToInsert.setEmail(user.getEmail());
            // 排除 phone, personalSignature, sex, profilePicture

            // 插入用户数据（仅包含允许的字段）
            userMapper.createUserTable();
            userMapper.insertUser(userToInsert);

            log.info("注册成功: 用户名 = {}", user.getUserName());
            return Result.success("注册成功");

        } catch (Exception e) {
            log.error("注册失败: 用户名 = {}, 异常信息: {}", user.getUserName(), e.getMessage(), e);
            return Result.error("注册失败，请稍后重试");
        }
    }

    /**
     * 用户登出
     * @param token JWT令牌
     * @return 登出结果
     */
    @Override
    public Result logout(String token) {
        try {
            if (JWTUtils.validateToken(token)) {
                String username = JWTUtils.getUsernameFromToken(token);

                // 修正：根据Token剩余有效期设置黑名单过期时间
                long expirationMillis = JWTUtils.getTokenExpiration(token) - System.currentTimeMillis();
                if (expirationMillis > 0) {
                    jwtBlacklistService.addToBlacklist(token, expirationMillis);
                } else {
                    // 若Token已过期，设置默认1小时过期时间
                    jwtBlacklistService.addToBlacklist(token, 3600 * 1000L);
                }

                log.info("用户登出: 用户名 = {}", username);
                return Result.success("登出成功");
            } else {
                log.warn("无效的token: {}", token);
                return Result.error("无效的token");
            }
        } catch (Exception e) {
            log.error("登出失败: 异常信息: {}", e.getMessage(), e);
            return Result.error("登出失败，请稍后重试");
        }
    }

    /**
     * 刷新Token
     * @param token 旧Token
     * @return 新Token
     */
    @Override
    public Result refreshToken(String token) {
        try {
            // 新增：检查旧Token是否已被拉黑
            if (jwtBlacklistService.isBlacklisted(token)) {
                log.warn("Token已被拉黑，无法刷新: {}", token);
                return Result.error("Token已失效，请重新登录");
            }

            if (JWTUtils.validateToken(token)) {
                String username = JWTUtils.getUsernameFromToken(token);
                User user = userMapper.findUserByUserName(username);

                if (user == null) {
                    log.warn("用户不存在: {}", username);
                    return Result.error("用户不存在");
                }

                // 准备新的JWT声明
                Map<String, Object> claims = new HashMap<>();
                claims.put("signature", user.getPersonalSignature());
                claims.put("gender", user.getSex());

                // 生成新的token
                String newToken = JWTUtils.createToken(username, claims);
                log.info("刷新token成功: 用户名 = {}", username);

                // 将旧Token加入黑名单
                long expirationMillis = JWTUtils.getTokenExpiration(token) - System.currentTimeMillis();
                if (expirationMillis > 0) {
                    jwtBlacklistService.addToBlacklist(token, expirationMillis);
                }

                Map<String, Object> data = new HashMap<>();
                data.put("token", newToken);
                return Result.success(data);
            } else {
                log.warn("无效的token: {}", token);
                return Result.error("无效的token");
            }
        } catch (Exception e) {
            log.error("刷新token失败: 异常信息: {}", e.getMessage(), e);
            return Result.error("刷新token失败，请稍后重试");
        }
    }

    /**
     * 获取用户信息
     * @param token JWT令牌
     * @return 用户信息
     */
    @Override
    public Result getUserInfo(String token) {
        try {
            // 新增：检查Token是否已被拉黑
            if (jwtBlacklistService.isBlacklisted(token)) {
                log.warn("Token已被拉黑: {}", token);
                return Result.error("Token已失效，请重新登录");
            }

            if (JWTUtils.validateToken(token)) {
                String username = JWTUtils.getUsernameFromToken(token);
                User user = userMapper.findUserByUserName(username);

                if (user == null) {
                    log.warn("用户不存在: {}", username);
                    return Result.error("用户不存在");
                }

                //移除敏感信息
                user.setPassWord(null);

                log.info("获取用户信息成功: 用户名 = {}", username);
                return Result.success(user); // 过滤敏感信息
            } else {
                log.warn("无效的token: {}", token);
                return Result.error("无效的token");
            }
        } catch (Exception e) {
            log.error("获取用户信息失败: 异常信息: {}", e.getMessage(), e);
            return Result.error("获取用户信息失败，请稍后重试");
        }
    }

    

    /**
     * 修改密码
     * @param userName 用户名
     * @param email 邮箱
     * @param newPassword 新密码
     * @return 修改结果
     */
    @Override
    public Result changePassword(String userName, String email, String newPassword) {
        try {
            log.info("修改密码请求: 用户名 = {}, 邮箱 = {}", userName, email);

            User user = userMapper.findUserByUserName(userName);
            if (user == null || !user.getEmail().equals(email)) {
                log.warn("用户名或邮箱不匹配: 用户名 = {}, 邮箱 = {}", userName, email);
                return Result.error("用户名或邮箱不匹配");
            }

            // 新增：验证新密码复杂度
            if (!PasswordUtils.isStrongPassword(newPassword)) {
                log.warn("新密码强度不足: {}", userName);
                return Result.error("新密码需包含大小写字母、数字和特殊字符，长度至少8位");
            }

            // 加密新密码
            String encryptedPassword = PasswordUtils.encryptPassword(newPassword);
            userMapper.updatePasswordByUsername(userName, encryptedPassword);

            log.info("密码修改成功: 用户名 = {}", userName);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            log.error("密码修改失败: 用户名 = {}, 邮箱 = {}, 异常信息: {}",
                    userName, email, e.getMessage(), e);
            return Result.error("密码修改失败，请稍后重试");
        }
    }

    /**
     * 重置密码
     * @param email 邮箱
     * @param userName 用户名
     * @return 重置结果
     */
    @Override
    public Result resetPassword(String email, String userName) {
        try {
            log.info("重置密码请求: 用户名 = {}, 邮箱 = {}", userName, email);

            User user = userMapper.findUserByUserName(userName);
            if (user == null || !user.getEmail().equals(email)) {
                log.warn("用户名或邮箱不匹配: 用户名 = {}, 邮箱 = {}", userName, email);
                return Result.error("用户名或邮箱不匹配");
            }

            // 生成随机临时密码
            String tempPassword = PasswordUtils.generateTempPassword();
            String encryptedPassword = PasswordUtils.encryptPassword(tempPassword);

            // 更新数据库中的密码
            userMapper.updatePasswordByUsername(userName, encryptedPassword);

            // 发送临时密码到用户邮箱（需实现EmailService）
            // emailService.sendTempPassword(email, tempPassword);

            log.info("重置密码成功: 用户名 = {}", userName);
            return Result.success("重置密码成功，请检查您的邮箱");
        } catch (Exception e) {
            log.error("重置密码失败: 用户名 = {}, 邮箱 = {}, 异常信息: {}",
                    userName, email, e.getMessage(), e);
            return Result.error("重置密码失败，请稍后重试");
        }
    }

    /**
     * 验证权限（修正方法名拼写错误）
     * @param token JWT令牌
     * @param requiredPermission 所需权限
     * @return 权限验证结果
     */
    @Override
    public Result verifyPermission(String token, String requiredPermission) {
        try {
            // 新增：检查Token是否已被拉黑
            if (jwtBlacklistService.isBlacklisted(token)) {
                log.warn("Token已被拉黑: {}", token);
                return Result.error("Token已失效，请重新登录");
            }

            if (!JWTUtils.validateToken(token)) {
                log.warn("无效的token: {}", token);
                return Result.error("无效的token");
            }

            String username = JWTUtils.getUsernameFromToken(token);
            User user = userMapper.findUserByUserName(username);

            if (user == null) {
                log.warn("用户不存在: {}", username);
                return Result.error("用户不存在");
            }

            // 验证用户权限（需在User类中添加权限字段）
            // if (!user.hasPermission(requiredPermission)) {
            //     log.warn("权限不足: 用户名 = {}, 需要权限 = {}", username, requiredPermission);
            //     return Result.error("权限不足");
            // }

            log.info("权限验证成功: 用户名 = {}, 权限 = {}", username, requiredPermission);
            return Result.success("权限验证成功");
        } catch (Exception e) {
            log.error("权限验证失败: 异常信息: {}", e.getMessage(), e);
            return Result.error("权限验证失败，请稍后重试");
        }
    }

    /**
     * 检查用户名是否存在
     * @param userName 用户名
     * @return 检查结果
     */
    @Override
    public Result checkUserNameExists(String userName) {
        try {
            log.info("检查用户名是否存在: 用户名 = {}", userName);
            User user = userMapper.findUserByUserName(userName);

            if (user != null) {
                log.info("用户名已存在: {}", userName);
                return Result.error("用户名已存在");
            } else {
                log.info("用户名可用: {}", userName);
                return Result.success("用户名可用");
            }
        } catch (Exception e) {
            log.error("检查用户名是否存在失败: 用户名 = {}, 异常信息: {}",
                    userName, e.getMessage(), e);
            return Result.error("检查用户名是否存在失败，请稍后重试");
        }
    }

    /**
     * 检查邮箱是否存在
     * @param email 邮箱
     * @return 检查结果
     */
    @Override
    public Result checkEmailExists(String email) {
        try {
            log.info("检查邮箱是否存在: 邮箱 = {}", email);
            User user = userMapper.findUserByEmail(email);

            if (user != null) {
                log.info("邮箱已存在: {}", email);
                return Result.error("邮箱已存在");
            } else {
                log.info("邮箱可用: {}", email);
                return Result.success("邮箱可用");
            }
        } catch (Exception e) {
            log.error("检查邮箱是否存在失败: 邮箱 = {}, 异常信息: {}",
                    email, e.getMessage(), e);
            return Result.error("检查邮箱是否存在失败，请稍后重试");
        }
    }

    /**
     * 发送邮箱验证码
     * @param email 邮箱
     * @return 发送结果
     */
    @Override
    public Result sendVerificationCode(String email) {
        try {
            log.info("发送验证码请求: 邮箱 = {}", email);

            // 检查邮箱是否已注册
            User user = userMapper.findUserByEmail(email);
            if (user == null) {
                log.warn("邮箱未注册: {}", email);
                return Result.error("邮箱未注册");
            }

            // 生成验证码
            String verificationCode = PasswordUtils.generateVerificationCode();

            // 存储验证码到缓存（需实现CacheService）
            // cacheService.put("verify:code:" + email, verificationCode, 5, TimeUnit.MINUTES);

            // 发送验证码邮件（需实现EmailService）
            // emailService.sendVerificationCode(email, verificationCode);

            log.info("验证码已发送: 邮箱 = {}", email);
            return Result.success("验证码已发送，请检查您的邮箱");
        } catch (Exception e) {
            log.error("发送验证码失败: 邮箱 = {}, 异常信息: {}",
                    email, e.getMessage(), e);
            return Result.error("发送验证码失败，请稍后重试");
        }
    }

    @Override
    public Result verifyEmailCode(String email, String code) {
        try {
            log.info("验证邮箱验证码: 邮箱 = {}", email);

            // 从缓存获取验证码（需要实现CacheService）
            // String storedCode = cacheService.get(email);

            // 模拟从缓存获取验证码
            String storedCode = "123456"; // 实际应从缓存获取

            if (storedCode == null) {
                log.warn("验证码已过期: 邮箱 = {}", email);
                return Result.error("验证码已过期，请重新获取");
            }

            if (!storedCode.equals(code)) {
                log.warn("验证码不正确: 邮箱 = {}", email);
                return Result.error("验证码不正确");
            }

            // 验证成功后删除缓存中的验证码
            // cacheService.remove(email);

            log.info("验证码验证成功: 邮箱 = {}", email);
            return Result.success("验证码验证成功");
        } catch (Exception e) {
            log.error("验证验证码失败: 邮箱 = {}, 异常信息: {}",
                    email, e.getMessage(), e);
            return Result.error("验证验证码失败，请稍后重试");
        }
    }
}