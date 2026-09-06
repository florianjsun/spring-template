package com.florian.sun.spring.template.application.file.adaptor;

import java.io.InputStream;
import java.util.Optional;

/**
 * 文件存储适配器接口
 * 屏蔽 S3 / MinIO 等对象存储细节，实现在 adaptor/file/output/S3FileStorageAdaptorImpl
 *
 * @author Florian Sun
 */
public interface FileStorageAdaptor {

    /**
     * 写入对象；失败抛 BizException(EXTERNAL_SERVICE_ERROR)，由调用方事务回滚元数据
     */
    void store(String storageKey, InputStream content, long size, String contentType);

    /**
     * 读取对象；对象不存在返回 empty，流由调用方负责关闭
     */
    Optional<InputStream> load(String storageKey);

    /**
     * 删除对象；对象不存在视为成功
     */
    void delete(String storageKey);
}
