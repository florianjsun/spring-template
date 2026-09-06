package com.florian.sun.spring.template.domain.file.model.param;

import com.florian.sun.spring.template.common.model.PageQuery;
import lombok.Getter;
import lombok.Setter;

/**
 * 分页查询文件条件
 *
 * @author Florian Sun
 */
@Getter
@Setter
public class PageFileQuery extends PageQuery {

    private Long uploaderId;
    /** 原始文件名模糊匹配 */
    private String keyword;
}
