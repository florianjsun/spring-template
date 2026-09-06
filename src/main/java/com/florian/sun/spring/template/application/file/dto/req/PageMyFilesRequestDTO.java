package com.florian.sun.spring.template.application.file.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.florian.sun.spring.template.common.model.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 分页查询我的文件请求
 *
 * @author Florian Sun
 */
@Getter
@Setter
@Schema(description = "分页查询我的文件请求")
public class PageMyFilesRequestDTO extends PageQuery {

    @Schema(description = "关键字，匹配原始文件名")
    @Size(max = 128, message = "关键字长度不能超过 128")
    private String keyword;

    /** 操作人，由 Controller 从登录态注入 */
    @JsonIgnore
    @Schema(hidden = true)
    private Long operatorId;
}
