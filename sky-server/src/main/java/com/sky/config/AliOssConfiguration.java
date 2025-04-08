package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliOssConfiguration {

    @Bean
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties){
        AliOssUtil aliOssUtil = new AliOssUtil();
        aliOssUtil.setAccessKeyId(aliOssProperties.getAccessKeyId());
        aliOssUtil.setEndpoint(aliOssProperties.getEndpoint());
        aliOssUtil.setBucketName(aliOssProperties.getBucketName());
        aliOssUtil.setAccessKeySecret(aliOssProperties.getAccessKeySecret());

        return aliOssUtil;

    }
}
