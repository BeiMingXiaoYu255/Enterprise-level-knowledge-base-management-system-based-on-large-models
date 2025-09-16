/*
package com.cjlu.finalversionwebsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

*/
/**
 * Redis配置类
 * 用于自定义RedisTemplate的序列化方式和连接属性
 *//*

@Configuration
public class RedisConfig {

    */
/**
     * 配置自定义的RedisTemplate
     * 解决默认序列化器导致的key乱码和value可读性差问题
     *
     * @param factory Redis连接工厂，由Spring自动注入
     * @return 配置好的RedisTemplate实例
     *//*

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建RedisTemplate实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 设置连接工厂
        template.setConnectionFactory(factory);

        // 创建字符串序列化器（用于key）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 创建JSON序列化器（用于value）
        // 优点：序列化后的数据为JSON格式，可读性好；支持复杂对象序列化
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // 配置key序列化器
        template.setKeySerializer(stringSerializer);
        // 配置hash结构的key序列化器
        template.setHashKeySerializer(stringSerializer);

        // 配置value序列化器
        template.setValueSerializer(jsonSerializer);
        // 配置hash结构的value序列化器
        template.setHashValueSerializer(jsonSerializer);

        // 设置默认序列化器（当未指定序列化器时使用）
        template.setDefaultSerializer(jsonSerializer);

        // 初始化RedisTemplate（必须调用，否则可能出现序列化异常）
        template.afterPropertiesSet();

        return template;
    }
}*/
