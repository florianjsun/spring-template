package com.florian.sun.spring.template.application.file.dto.res;

import java.io.InputStream;

/**
 * 文件内容（下载用）
 * Controller 负责把 content 写入响应并关闭
 *
 * @param originalName 原始文件名
 * @param contentType  MIME 类型
 * @param fileSize     字节数
 * @param content      文件内容流
 * @author Florian Sun
 */
public record FileContentResponseDTO(String originalName, String contentType, Long fileSize, InputStream content) {
}
