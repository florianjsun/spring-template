package com.florian.sun.spring.template.domain.user.model.param;

import lombok.Data;

/**
 * 禁用用户参数
 *
 * @author Florian Sun
 */
@Data
public class DisableUserParam {

    private Long userId;
    /** 操作人，用于禁止禁用自己 */
    private Long operatorId;
}
