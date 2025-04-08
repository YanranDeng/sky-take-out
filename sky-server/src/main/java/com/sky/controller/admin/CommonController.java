package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/admin/common")
@Api(tags = "文件上传通用接口")
public class CommonController {

    @Autowired
    private AliOssUtil aliOssUtil;

    /**
     * 上传文件至AliOss,并返回该文件在AliOss所在的存储位置
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @ApiOperation("上传文件至AliOss")
    // 使用MultipartFile类型接收前端传入的文件
    public Result<String> update(MultipartFile file) {
        // 判断文件是否为空
        if (file == null) {
            return Result.error("传入文件不存在");
        }

        // 获取文件原始名
        String originName = file.getOriginalFilename();
        log.info("originName:{}",originName);
        // 获得文件的拓展名,这里使用"."分割字符串时需要在"."的前面加上"\\"表示转义.
        String extension = originName.split("\\.")[1];
        // 使用uuid给文件重新命名,避免oss中的文件名字重复
        StringBuilder sb = new StringBuilder();
        String newName = sb.append(UUID.randomUUID().toString()).append(".").append(extension).toString();
        try {
            // 调用AliOssUtil的update方法,传入文件的bytes数组格式的数据和文件的新命名,返回值是oss对于该文件的储存地址.
            String filePath = aliOssUtil.upload(file.getBytes(), newName);
            log.info(filePath);
            // 向前端返回存储地址用于前端回显.
            return Result.success(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
