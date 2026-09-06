package com.florian.sun.spring.template.application.auth.adaptor;

/**
 * 登录会话（Sa-Token token 信息的应用层表达）
 *
 * @param tokenName      请求头名称
 * @param tokenValue     token 值
 * @param timeoutSeconds 剩余有效期（秒）
 * @author Florian Sun
 */
public record SessionDTO(String tokenName, String tokenValue, long timeoutSeconds) {
}
