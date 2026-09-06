package com.florian.sun.spring.template.domain.user.model.param;

import lombok.Data;

/**
 * 修改资料参数
 *
 * @author Florian Sun
 */
@Data
public class UpdateProfileParam {

    private Long userId;
    private String nickname;
    /** 头像文件 ID，可为空表示清除头像 */
    private Long avatarFileId;
}
