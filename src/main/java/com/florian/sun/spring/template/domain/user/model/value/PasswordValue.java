package com.florian.sun.spring.template.domain.user.model.value;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.florian.sun.spring.template.common.exception.BizAssert;
import com.florian.sun.spring.template.domain.user.model.enums.UserErrorCode;

import java.nio.charset.StandardCharsets;

/**
 * 密码值对象
 * 只持有 BCrypt 哈希；落库为 t_user.password_hash 单列
 *
 * @param hash BCrypt 哈希
 * @author Florian Sun
 */
public record PasswordValue(String hash) {

    /**
     * BCrypt 只取前 72 字节，超长部分会被静默截断，这里显式拒绝
     */
    private static final int MAX_RAW_BYTES = 72;

    public PasswordValue {
        BizAssert.notBlank(hash, UserErrorCode.PASSWORD_INVALID);
    }

    /**
     * 明文 → 哈希，创建用户 / 修改密码时使用
     */
    public static PasswordValue encode(String rawPassword) {
        BizAssert.notBlank(rawPassword, UserErrorCode.PASSWORD_INVALID);
        BizAssert.isTrue(rawPassword.getBytes(StandardCharsets.UTF_8).length <= MAX_RAW_BYTES, UserErrorCode.PASSWORD_INVALID);
        return new PasswordValue(DigestUtil.bcrypt(rawPassword));
    }

    public boolean matches(String rawPassword) {
        return StrUtil.isNotBlank(rawPassword) && DigestUtil.bcryptCheck(rawPassword, hash);
    }
}
