package com.florian.sun.spring.template.adaptor.file.input;

import cn.dev33.satoken.stp.StpUtil;
import com.florian.sun.spring.template.application.file.dto.req.PageMyFilesRequestDTO;
import com.florian.sun.spring.template.application.file.dto.req.UploadFileRequestDTO;
import com.florian.sun.spring.template.application.file.dto.res.FileContentResponseDTO;
import com.florian.sun.spring.template.application.file.dto.res.FileInfoResponseDTO;
import com.florian.sun.spring.template.application.file.scenario.FileAppService;
import com.florian.sun.spring.template.application.file.scenario.FileQueryAppService;
import com.florian.sun.spring.template.common.result.PageResult;
import com.florian.sun.spring.template.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 文件接口
 * 下载接口是 Result<T> 的唯一例外：直接返回文件流
 *
 * @author Florian Sun
 */
@Tag(name = "文件", description = "上传 / 下载 / 我的文件")
@Validated
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileAppService fileAppService;
    private final FileQueryAppService fileQueryAppService;

    @Operation(summary = "上传文件")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FileInfoResponseDTO> uploadFile(@RequestPart("file") MultipartFile file) throws IOException {
        UploadFileRequestDTO dto = new UploadFileRequestDTO();
        dto.setOriginalName(file.getOriginalFilename());
        dto.setContentType(file.getContentType());
        dto.setFileSize(file.getSize());
        dto.setContent(file.getInputStream());
        dto.setOperatorId(StpUtil.getLoginIdAsLong());
        return Result.success(fileAppService.uploadFile(dto));
    }

    @Operation(summary = "查看文件信息")
    @GetMapping("/{fileId}")
    public Result<FileInfoResponseDTO> getFileInfo(@PathVariable @Min(1) Long fileId) {
        return Result.success(fileQueryAppService.getFileInfo(fileId));
    }

    @Operation(summary = "下载文件")
    @GetMapping("/{fileId}/download")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable @Min(1) Long fileId) {
        FileContentResponseDTO content = fileQueryAppService.downloadFile(fileId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(content.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.fileSize())
                .body(new InputStreamResource(content.content()));
    }

    @Operation(summary = "分页查询我的文件")
    @GetMapping
    public Result<PageResult<FileInfoResponseDTO>> pageMyFiles(@Valid @ParameterObject PageMyFilesRequestDTO dto) {
        dto.setOperatorId(StpUtil.getLoginIdAsLong());
        return Result.success(fileQueryAppService.pageMyFiles(dto));
    }

    @Operation(summary = "删除我的文件")
    @DeleteMapping("/{fileId}")
    public Result<Void> deleteFile(@PathVariable @Min(1) Long fileId) {
        fileAppService.deleteFile(fileId, StpUtil.getLoginIdAsLong());
        return Result.success();
    }
}
