package com.florian.sun.spring.template.infrastructure.file.mysql.po;

import com.florian.sun.spring.template.infrastructure.common.po.BasePO;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件表 t_file
 *
 * @author Florian Sun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_file")
public class FilePO extends BasePO {

    private String originalName;
    private String storageKey;
    private String contentType;
    private Long fileSize;
    private Long uploaderId;
}
