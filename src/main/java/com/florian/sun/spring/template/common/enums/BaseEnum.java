package com.florian.sun.spring.template.common.enums;

/**
 * 枚举基础接口
 * code 用于落库与前后端交互，description 用于展示
 *
 * @author Florian Sun
 */
public interface BaseEnum<C> {

    C getCode();

    String getDescription();

    /** 按 code 查找枚举，找不到返回 null */
    static <C, E extends Enum<E> & BaseEnum<C>> E of(Class<E> enumClass, C code) {
        if (code == null) {
            return null;
        }
        for (E e : enumClass.getEnumConstants()) {
            if (code.equals(e.getCode())) {
                return e;
            }
        }
        return null;
    }
}
