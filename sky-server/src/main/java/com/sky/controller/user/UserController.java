package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.properties.JwtProperties;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Api(tags = "微信用户相关接口")
@Slf4j
@RequestMapping("/user/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 微信用户登录
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("微信用户登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("登录对象的登录凭证为:{}",userLoginDTO.getCode());

        // 调用Service层的wxLogin方法,返回查询/添加的user对象
        User user = userService.wxLogin(userLoginDTO);

        // 新建一个Map集合,将Jwt的载荷写入,微信登录的jwt令牌的载荷约定为微信用户的id
        Map<String, Object> map = new HashMap<>();
        map.put(JwtClaimsConstant.USER_ID, user.getId());
        log.info("载荷Map的大小:{}",map.size());
        // 构建jwt令牌
        String jwt = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), map);

        // 创建一个UserLoginVO类用以写入前端需要响应的属性
        UserLoginVO userLoginVO = new UserLoginVO();
        // 写入三个属性,分别是微信用户在user表中的主键id,微信用户的openid,jwt令牌
        userLoginVO.setId(user.getId());
        userLoginVO.setOpenid(userLoginVO.getOpenid());
        userLoginVO.setToken(jwt);

        // 返回VO对象
        return Result.success(userLoginVO);
    }
}
