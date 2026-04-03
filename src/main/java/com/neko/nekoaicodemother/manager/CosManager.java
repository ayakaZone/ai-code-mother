package com.neko.nekoaicodemother.manager;

import com.neko.nekoaicodemother.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传文件对象
     * @param key 对象键
     * @param file 本地文件
     * @return PutObjectResult
     */
    private PutObjectResult putObject(String key, File file) {
        // 上传请求
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 调用 COS 客户端上传文件
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传文件并返回 COS 访问路径
     * @param key 对象键
     * @param file 本地文件
     * @return COS 访问路径
     */
    public String uploadFile(String key, File file) {
        // 上传文件
        PutObjectResult result = putObject(key, file);
        if (result != null) {
            // 上传成功，返回 COS 访问路径
            String cosUrl = String.format("%s%s", cosClientConfig.getHost(), key);
            log.info("上传文件成功, COS 访问路径 -> {}", cosUrl);
            return cosUrl;
        } else {
            // 上传失败
            log.error("上传文件失败");
            return null;
        }
    }
}
