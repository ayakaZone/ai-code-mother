package com.neko.nekoaicodemother.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云对象存储配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cos.client")
public class CosClientConfig {

    /**
     * 腾讯云对象存储服务域名
     */
    private String host;

    /**
     * 腾讯云对象存储服务密钥Id
     */
    private String secretId;

    /**
     * 腾讯云对象存储服务密钥Key
     */
    private String secretKey;

    /**
     * 腾讯云对象存储服务区域
     */
    private String region;

    /**
     * 腾讯云对象存储服务桶名称
     */
    private String bucket;

    /**
     * 创建腾讯云对象存储服务客户端(参考官方文档)
     * @return COSClient
     */
    @Bean
    public COSClient cosClient() {
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        return new COSClient(cred, clientConfig);
    }

}
