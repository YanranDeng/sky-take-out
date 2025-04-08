package com.sky.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建redis模板类");
        RedisTemplate redisTemplate = new RedisTemplate();

        // 设置序列化器,RedisTemplate默认的序列化器为JdkSerializationRedisSerializer,
        // 序列化器的作用是将java代码控制的redis数据在交互的时候将key序列化,以确保不会出现乱码
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // 给redisTemplate设置连接工厂参数
        redisTemplate.setConnectionFactory(redisConnectionFactory);



        return redisTemplate;

    }
}
