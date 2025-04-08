package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatProperties weChatProperties;

    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {

        // 通过getWxUserObject方法,传入登录用户DTO对象UserLoginDTO对象(主要是该对象的登录凭证code)获取登录用户的一系列信息,获得的信息封装在一个JsonObject对象中(Json对象)
        JSONObject jsonObject = getWxUserObject(userLoginDTO);
        // 通过key来获取json对象中的值,并将其转化为String类型.
        String openid = jsonObject.getString("openid");

        // 2 判断解析的openid是否为空,若为空则直接抛出异常,后续直接由全局异常处理器处理
        if (openid == null || openid.isEmpty()) {
            throw new LoginFailedException("登录失败");
        }

        // 3 使用用户的openid在user表中查找user数据,并封装到User类中返回
        User user = userMapper.findByOpenId(openid);

        // 3.1 判断查询返回的user是否为空,若不为空则直接返回user对象
        if (user != null) {
            return user;
        }
        // 3.2 若user为空,则表示该微信用户是第一次登录本系统,属于新用户需要先进行注册(在user表中插入数据)再返回数据,
        // 具体是将openid封装到User对象中传给insert方法,再user表中插入.
        // 由于目前无法获取微信用户的除openid以外的数据,因此目前只能再表中添加openid属性.
        User user1 = User.builder().openid(openid)
                .createTime(LocalDateTime.now())
                .build();
        userMapper.insert(user1);
        return user1;
    }


    public JSONObject getWxUserObject(UserLoginDTO userLoginDTO) {
        // 1 根据传入的userLoginDTO中的临时登录凭证code来请求微信接口code2Session来获取当前登录的微信用户的openid.
        // 创建一个Map集合用于存储请求微信接口code2Session的请求参数,
        // 该请求为get请求,共有四个请求参数,分别是appid(小程序 appId),secret(小程序 appSecret),
        // js_code(临时登录凭证),以及固定授权类型grant_type(固定为authorization_code).
        // https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("appid", weChatProperties.getAppid());
        paramMap.put("secret", weChatProperties.getSecret());
        paramMap.put("js_code", userLoginDTO.getCode());
        paramMap.put("grant_type", "authorization_code");
        // 使用自定义HttpClientUtil工具类的doGet方法向code2Session接口发出get请求,该方法传入两个参数,分别是请求路径和请求参数的集合.
        // 返回值是微信接口响应的json对象的字符串数据,包括用户的openid等五个数据
        String response = HttpClientUtil.doGet("https://api.weixin.qq.com/sns/jscode2session", paramMap);

        // 使用fastJson包中的Json类对返回的字符串进行解析,将其解析为json对象.
        JSONObject jsonObject = JSON.parseObject(response);
        return jsonObject;
    }
}
