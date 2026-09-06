package com.florian.sun.spring.template.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * 分页查询基类
 * application/dto/req 的分页 RequestDTO 与 domain/model/param 的分页 Query 都继承它；
 * 校验注解只在被 Controller @Valid 触发时生效
 *
 * @author Florian Sun
 */
@Getter
@Setter
public abstract class PageQuery {

    private static final long MAX_PAGE_SIZE = 200;

    @Min(value = 1, message = "页码最小为 1")
    private long pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为 200")
    private long pageSize = 10;
}
