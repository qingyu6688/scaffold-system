package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.mapper.AuthMapper;
import com.scaffold.system.mapper.SystemManagementMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 统一用户写入边界。调用方必须在事务内锁定目标用户后再写入。 */
@Service
public class UserAccessPolicy {
    private final AuthMapper authMapper;
    private final SystemManagementMapper mapper;
    private final SystemDataScopeResolver scopeResolver;

    public UserAccessPolicy(AuthMapper authMapper, SystemManagementMapper mapper, SystemDataScopeResolver scopeResolver) {
        this.authMapper = authMapper;
        this.mapper = mapper;
        this.scopeResolver = scopeResolver;
    }

    public boolean isSuperAdmin() {
        var actor = authMapper.selectUserByUsername(username());
        return actor != null && Integer.valueOf(1).equals(actor.getStatus())
                && authMapper.selectRoleCodes(actor.getId()).contains("super_admin");
    }

    public void requireSuperAdmin() {
        if (!isSuperAdmin()) {
            throw new BusinessException(403, "角色、菜单和部门结构的维护仅允许超级管理员操作");
        }
    }

    public Map<String, Object> requireWritableUser(Long id) {
        var target = mapper.selectUserForUpdate(id);
        if (target == null) throw new BusinessException("用户不存在或已删除");
        if (!isSuperAdmin()) {
            var scope = scopeResolver.resolve(username());
            boolean own = Objects.equals(scope.getUserId(), id);
            boolean sameScope = scope.isAllScope() || scope.getDeptIds().contains(number(target.get("deptId")));
            if ((!own && !sameScope) || mapper.selectUserRoleCodes(id).contains("super_admin")) {
                throw new BusinessException(403, "无权操作该用户");
            }
            var actor = authMapper.selectUserByUsername(username());
            var targetScope = scopeResolver.resolve(String.valueOf(target.get("username")));
            if (actor == null || !new HashSet<>(authMapper.selectPermissions(actor.getId()))
                    .containsAll(authMapper.selectPermissions(id))
                    || (!scope.isAllScope() && (targetScope.isAllScope()
                    || !scope.getDeptIds().containsAll(targetScope.getDeptIds())))) {
                throw new BusinessException(403, "不能修改权限或数据范围高于本人的用户");
            }
        }
        return target;
    }

    public void checkDepartment(Long deptId) {
        if (deptId == null) return;
        if (mapper.countActiveDept(deptId) != 1) throw new BusinessException("所属部门不存在或已停用");
        var scope = scopeResolver.resolve(username());
        if (!isSuperAdmin() && !scope.isAllScope() && !scope.getDeptIds().contains(deptId)) {
            throw new BusinessException(403, "无权将用户分配到该部门");
        }
    }

    public void checkRoles(Long targetId, Long deptId, List<Long> roleIds) {
        if (roleIds == null) return;
        if (new HashSet<>(roleIds).size() != roleIds.size()) throw new BusinessException("角色不能重复");
        var currentIds = targetId == null ? List.<Long>of() : mapper.selectUserRoleIds(targetId);
        if (new HashSet<>(currentIds).equals(new HashSet<>(roleIds))) return;
        for (Long roleId : roleIds) {
            if (mapper.countActiveRole(roleId) != 1) throw new BusinessException("角色不存在或已停用");
        }
        if (isSuperAdmin()) return;
        var actor = authMapper.selectUserByUsername(username());
        // 按本人角色委派只允许同部门，避免将部门范围角色授予其他部门后扩大可见数据。
        if (actor == null || !Objects.equals(actor.getDeptId(), deptId)
                || !new HashSet<>(mapper.selectUserRoleIds(actor.getId())).containsAll(roleIds)) {
            throw new BusinessException(403, "只能向同部门用户授予本人持有的角色");
        }
        if (roleIds.stream().anyMatch(id -> mapper.isSuperAdminRole(id))) {
            throw new BusinessException(403, "无权授予超级管理员角色");
        }
    }

    public void checkPosts(List<Long> postIds) {
        checkPosts(null, postIds);
    }

    public void checkPosts(Long targetId, List<Long> postIds) {
        if (postIds == null) return;
        List<Long> existing = targetId == null ? List.of() : mapper.selectUserPostIds(targetId);
        if (new HashSet<>(postIds).size() != postIds.size()) throw new BusinessException("岗位不能重复");
        for (Long id : postIds) {
            if (!existing.contains(id) && mapper.countActivePost(id) != 1) throw new BusinessException("岗位不存在或已停用");
        }
    }

    public List<Long> assignableRoleIds() {
        var actor = authMapper.selectUserByUsername(username());
        return actor == null ? List.of() : mapper.selectUserRoleIds(actor.getId());
    }

    private String username() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new BusinessException(401, "请先登录");
        return String.valueOf(authentication.getPrincipal());
    }

    private Long number(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
