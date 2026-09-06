package com.florian.sun.spring.template.application.file.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息
 *
 * @author Florian Sun
 */
@Data
@Schema(description = "文件信息")
public class FileInfoResponseDTO {

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "MIME 类型")
    private String contentType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "上传者ID")
    private Long uploaderId;

    @Schema(description = "上传时间")
    private LocalDateTime createTime;
}
