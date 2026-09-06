package com.florian.sun.spring.template.domain.file.model.param;

import lombok.Data;

/**
 * 删除文件参数
 *
 * @author Florian Sun
 */
@Data
public class RemoveFileParam {

    private Long fileId;
    /** 操作人，只有上传者本人可删除 */
    private Long operatorId;
}
