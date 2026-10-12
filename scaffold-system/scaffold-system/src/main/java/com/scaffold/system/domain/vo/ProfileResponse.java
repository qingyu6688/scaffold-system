package com.scaffold.system.domain.vo;

import java.util.List;
import java.util.Map;

/**
 * 个人中心资料。
 *
 * @param userId 用户 ID
 * @param username 登录账号，只读
 * @param nickname 用户昵称
 * @param phone 联系电话
 * @param email 邮箱
 * @param deptName 所属部门名称，只读
 * @param roles 已启用角色名称，只读
 * @param posts 已启用岗位名称，只读
 * @param lastLoginTime 最近登录时间
 * @param createTime 账号创建时间
 * @param recentLogins 本账号最近登录记录：time 时间、status 1 成功 / 0 失败、msg 提示消息
 */
public record ProfileResponse(
        Long userId,
        String username,
        String nickname,
        String phone,
        String email,
        String deptName,
        List<String> roles,
        List<String> posts,
        String lastLoginTime,
        String createTime,
        List<Map<String, Object>> recentLogins
) {
}
