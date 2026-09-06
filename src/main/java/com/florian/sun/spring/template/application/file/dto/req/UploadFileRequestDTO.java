package com.florian.sun.spring.template.application.file.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.io.InputStream;

/**
 * 上传文件请求
 * 由 Controller 从 MultipartFile 组装，不直接绑定 HTTP 请求体
 *
 * @author Florian Sun
 */
@Data
@Schema(hidden = true)
public class UploadFileRequestDTO {

    private String originalName;
    private String contentType;
    /** 字节数 */
    private Long fileSize;
    @ToString.Exclude
    private InputStream content;
    /** 操作人，由 Controller 从登录态注入 */
    private Long operatorId;
}
