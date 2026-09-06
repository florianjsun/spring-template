package com.florian.sun.spring.template.domain.file.model.enums;

import com.florian.sun.spring.template.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文件领域错误码
 *
 * @author Florian Sun
 */
@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

    FILE_NOT_FOUND("FILE_NOT_FOUND", "文件不存在"),
    FILE_NAME_BLANK("FILE_NAME_BLANK", "文件名不能为空"),
    FILE_EMPTY("FILE_EMPTY", "上传文件不能为空"),
    NOT_FILE_OWNER("NOT_FILE_OWNER", "无权操作他人文件"),
    /** 元数据存在但对象存储中找不到内容 */
    FILE_CONTENT_NOT_FOUND("FILE_CONTENT_NOT_FOUND", "文件内容不存在"),
    ;

    private final String code;
    private final String message;
}
