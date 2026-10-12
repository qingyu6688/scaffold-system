package com.scaffold.system.controller;

import com.scaffold.common.domain.R;
import com.scaffold.framework.audit.OperLog;
import com.scaffold.system.domain.dto.ProfileUpdateRequest;
import com.scaffold.system.domain.vo.ProfileResponse;
import com.scaffold.system.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 个人中心只要求已登录，不挂按钮权限：每个账号都能维护自己的资料。 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public R<ProfileResponse> profile() {
        return R.success(profileService.profile());
    }

    @PutMapping
    @OperLog(module = "个人中心", action = "修改资料")
    public R<ProfileResponse> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return R.success(profileService.updateProfile(request));
    }
}
