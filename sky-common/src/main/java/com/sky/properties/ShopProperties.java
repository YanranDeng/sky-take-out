package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 将百度地图的相关属性注入到ShopProperties属性类中
@Component
@ConfigurationProperties(prefix = "sky.shop")
@Data
public class ShopProperties {
    private String address;
    private String ak;
}
