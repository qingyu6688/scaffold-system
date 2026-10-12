package com.scaffold.system.service;

import com.scaffold.common.exception.BusinessException;
import com.scaffold.system.domain.ProfileRow;
import com.scaffold.system.domain.dto.ProfileUpdateRequest;
import com.scaffold.system.domain.vo.ProfileResponse;
import com.scaffold.system.mapper.ProfileMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** 个人中心：只读写当前登录账号本人的资料，不经过用户管理的权限与数据范围。 */
@Service
public class ProfileService {

    private static final int RECENT_LOGIN_LIMIT = 6;

    private final ProfileMapper profileMapper;

    public ProfileService(ProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    public ProfileResponse profile() {
        ProfileRow row = currentProfileRow();
        return new ProfileResponse(
                row.getId(),
                row.getUsername(),
                row.getNickname(),
                row.getPhone(),
                row.getEmail(),
                row.getDeptName(),
                profileMapper.selectRoleNames(row.getId()),
                profileMapper.selectPostNames(row.getId()),
                row.getLastLoginTime(),
                row.getCreateTime(),
                profileMapper.selectRecentLogins(row.getUsername(), RECENT_LOGIN_LIMIT)
        );
    }

    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        ProfileRow row = currentProfileRow();
        int updated = profileMapper.updateProfile(row.getId(), row.getUsername(),
                request.getNickname().trim(), blankToNull(request.getPhone()), blankToNull(request.getEmail()));
        if (updated == 0) {
            throw new BusinessException("个人资料保存失败，请刷新后重试");
        }
        return profile();
    }

    private ProfileRow currentProfileRow() {
        String username = String.valueOf(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        ProfileRow row = profileMapper.selectProfile(username);
        if (row == null) {
            throw new BusinessException(401, "未认证或登录已过期");
        }
        return row;
    }

    /** 清空联系方式时存 NULL，与用户管理中未填写的状态保持一致 */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
