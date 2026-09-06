package com.florian.sun.spring.template.adaptor.file.output;

import com.florian.sun.spring.template.application.file.adaptor.FileStorageAdaptor;
import com.florian.sun.spring.template.common.exception.BizException;
import com.florian.sun.spring.template.common.exception.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.Optional;

/**
 * 文件存储适配器 S3 / MinIO 实现
 * SDK 异常统一转 BizException(EXTERNAL_SERVICE_ERROR)，对象不存在转 Optional.empty()
 *
 * @author Florian Sun
 */
@Slf4j
@Component
public class S3FileStorageAdaptorImpl implements FileStorageAdaptor {

    private final S3Client s3Client;
    private final String bucket;

    public S3FileStorageAdaptorImpl(S3Client s3Client, @Value("${file.storage.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void store(String storageKey, InputStream content, long size, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType)
                .contentLength(size)
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromInputStream(content, size));
        } catch (SdkException e) {
            log.error("对象存储写入失败 bucket={}, key={}", bucket, storageKey, e);
            throw new BizException(CommonErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }

    @Override
    public Optional<InputStream> load(String storageKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();
        try {
            return Optional.of(s3Client.getObject(request));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (SdkException e) {
            log.error("对象存储读取失败 bucket={}, key={}", bucket, storageKey, e);
            throw new BizException(CommonErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();
        try {
            s3Client.deleteObject(request);
        } catch (SdkException e) {
            log.error("对象存储删除失败 bucket={}, key={}", bucket, storageKey, e);
            throw new BizException(CommonErrorCode.EXTERNAL_SERVICE_ERROR, e);
        }
    }
}
