package com.florian.sun.spring.template.common.validation;

/**
 * 校验分组
 * 同一个 RequestDTO 在新增与修改场景校验规则不同时使用
 *
 * @author Florian Sun
 */
public interface ValidGroup {

    /** 新增 */
    interface Create {
    }

    /** 修改 */
    interface Update {
    }
}
