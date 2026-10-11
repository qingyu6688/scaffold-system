package com.scaffold.framework.security;

/** 查询当前账号状态和会话版本，所有已认证接口均须校验。 */
public interface SessionAccountResolver {
    boolean isActive(String username, long version);
}
