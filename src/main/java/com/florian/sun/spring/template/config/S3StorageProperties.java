package com.florian.sun.spring.template.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3 / MinIO 对象存储配置
 * 对应 application.yaml 的 file.storage.s3
 *
 * @param endpoint        服务地址，如 http://localhost:9000
 * @param region          区域，MinIO 任意填写
 * @param accessKey       访问密钥
 * @param secretKey       私有密钥
 * @param bucket          存储桶，需预先创建
 * @param pathStyleAccess 是否使用 path-style 访问（MinIO 为 true）
 * @author Florian Sun
 */
@ConfigurationProperties(prefix = "file.storage.s3")
public record S3StorageProperties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String bucket,
        boolean pathStyleAccess
) {
}
