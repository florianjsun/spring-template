package com.florian.sun.spring.template.domain.file.model.param;

import lombok.Data;

/**
 * 创建文件元数据参数
 *
 * @author Florian Sun
 */
@Data
public class CreateFileParam {

    private String originalName;
    private String contentType;
    /** 字节数 */
    private Long fileSize;
    private Long uploaderId;
}
