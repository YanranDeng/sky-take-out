package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface UserMapper {
    @Select("select id, openid, name, phone, sex, id_number, avatar, create_time from user where openid =#{openid}")
    User findByOpenId(@Param("openid") String openid);

    void insert(User user1);

    @Select("select * from user where id=#{userId}")
    User getById(Long userId);

    /**
     * 根据传入map中的查询时间来计算用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
