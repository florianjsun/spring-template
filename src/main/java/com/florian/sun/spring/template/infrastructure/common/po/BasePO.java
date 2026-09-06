package com.florian.sun.spring.template.infrastructure.common.po;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * PO 基类
 * 统一主键、审计字段、逻辑删除、乐观锁；所有表都带这四类列，所有 PO 都继承它
 *
 * @author Florian Sun
 */
@Data
public abstract class BasePO {

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 乐观锁，MyBatis-Flex 更新时自动带 version 条件并 +1 */
    @Column(version = true)
    private Integer version;

    /** 逻辑删除：0 正常 1 删除，查询自动追加 deleted = 0 */
    @Column(isLogicDelete = true)
    private Integer deleted;

    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;

    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
}
