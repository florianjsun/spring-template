package com.florian.sun.spring.template.common.result;

import com.florian.sun.spring.template.common.model.PageQuery;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.function.Function;

/**
 * 分页结果
 * 跨层分页载体：Repository 返回 PageResult<聚合根>，Assembler 转成 PageResult<ResponseDTO>
 *
 * @author Florian Sun
 */
@Getter
@ToString
public class PageResult<T> {

    private final long pageNum;
    private final long pageSize;
    private final long total;
    private final List<T> records;

    private PageResult(long pageNum, long pageSize, long total, List<T> records) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.records = records == null ? List.of() : records;
    }

    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> records) {
        return new PageResult<>(pageNum, pageSize, total, records);
    }

    public static <T> PageResult<T> empty(PageQuery query) {
        return new PageResult<>(query.getPageNum(), query.getPageSize(), 0, List.of());
    }

    /**
     * 保留分页信息，仅转换记录类型（Assembler 中使用）
     */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(pageNum, pageSize, total, records.stream().map(mapper).toList());
    }
}
