package com.florian.sun.spring.template.domain.file.model.entity;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.florian.sun.spring.template.common.exception.BizAssert;
import com.florian.sun.spring.template.common.model.BaseEntity;
import com.florian.sun.spring.template.domain.file.model.enums.FileErrorCode;
import com.florian.sun.spring.template.domain.file.model.param.CreateFileParam;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 文件根实体
 * 与 t_file 一一对应，只持有元数据；文件内容存对象存储，由 storageKey 定位
 *
 * @author Florian Sun
 */
@Getter
@Setter
public class FileEntity extends BaseEntity {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String STORAGE_DATE_PATTERN = "yyyy/MM/dd";

    private String originalName;
    private String storageKey;
    private String contentType;
    private Long fileSize;
    private Long uploaderId;

    /**
     * 创建元数据：去掉客户端可能带上的路径，生成 yyyy/MM/dd/{snowflake}.{ext} 形式的对象 key
     * FileNameUtil 只做文件名字符串处理，不涉及 IO
     */
    public static FileEntity create(CreateFileParam param) {
        BizAssert.notBlank(param.getOriginalName(), FileErrorCode.FILE_NAME_BLANK);
        BizAssert.isTrue(param.getFileSize() != null && param.getFileSize() > 0, FileErrorCode.FILE_EMPTY);

        String originalName = FileNameUtil.getName(param.getOriginalName());
        BizAssert.notBlank(originalName, FileErrorCode.FILE_NAME_BLANK);

        FileEntity file = new FileEntity();
        file.originalName = originalName;
        file.storageKey = buildStorageKey(originalName);
        file.contentType = StrUtil.blankToDefault(param.getContentType(), DEFAULT_CONTENT_TYPE);
        file.fileSize = param.getFileSize();
        file.uploaderId = param.getUploaderId();
        return file;
    }

    public void ensureOwnedBy(Long operatorId) {
        BizAssert.isTrue(isOwnedBy(operatorId), FileErrorCode.NOT_FILE_OWNER);
    }

    public boolean isOwnedBy(Long operatorId) {
        return Objects.equals(uploaderId, operatorId);
    }

    private static String buildStorageKey(String originalName) {
        String datePath = LocalDateTimeUtil.format(LocalDateTime.now(), STORAGE_DATE_PATTERN);
        String extension = FileNameUtil.extName(originalName).toLowerCase();
        String suffix = extension.isEmpty() ? "" : "." + extension;
        return datePath + "/" + IdUtil.getSnowflakeNextIdStr() + suffix;
    }
}
