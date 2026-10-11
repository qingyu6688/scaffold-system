package com.scaffold.system.service;

import com.scaffold.framework.security.PermissionResolver;
import com.scaffold.system.mapper.AuthMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class SystemPermissionResolver implements PermissionResolver {

    private static final String SUPER_ADMIN = "super_admin";
    private static final Set<String> SECURITY_DEFINITION_WRITES = Set.of(
            "system:role:add", "system:role:edit", "system:role:remove",
            "system:menu:add", "system:menu:edit", "system:menu:remove",
            "system:dept:add", "system:dept:edit", "system:dept:remove");

    /** 无委派授权模型时，安全定义的写权限仅由超级管理员持有。 */
    public static List<String> effectivePermissions(List<String> permissions) {
        return permissions.stream().filter(permission -> !SECURITY_DEFINITION_WRITES.contains(permission)).toList();
    }

    private final AuthMapper authMapper;

    public SystemPermissionResolver(AuthMapper authMapper) {
        this.authMapper = authMapper;
    }

    @Override
    public boolean hasPermission(String username, String permission) {
        var user = authMapper.selectUserByUsername(username);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            return false;
        }
        if (authMapper.selectRoleCodes(user.getId()).contains(SUPER_ADMIN)) {
            return true;
        }
        return effectivePermissions(authMapper.selectPermissions(user.getId())).contains(permission);
    }
}
