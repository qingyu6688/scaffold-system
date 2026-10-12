package com.scaffold.system.mapper;

import com.scaffold.system.domain.ProfileRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProfileMapper {

    @Select("""
            SELECT u.id, u.username, u.nickname, u.phone, u.email, d.dept_name,
                   DATE_FORMAT(u.last_login_time, '%Y-%m-%d %H:%i') AS lastLoginTime,
                   DATE_FORMAT(u.create_time, '%Y-%m-%d %H:%i') AS createTime
            FROM sys_user u
            LEFT JOIN sys_dept d ON d.id = u.dept_id
            WHERE u.username = #{username} AND u.deleted = 0
            LIMIT 1
            """)
    ProfileRow selectProfile(@Param("username") String username);

    @Select("""
            SELECT r.role_name
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0
            ORDER BY r.sort, r.id
            """)
    List<String> selectRoleNames(@Param("userId") Long userId);

    @Select("""
            SELECT p.post_name
            FROM sys_user_post up
            JOIN sys_post p ON p.id = up.post_id
            WHERE up.user_id = #{userId} AND p.status = 1 AND p.deleted = 0
            ORDER BY p.sort, p.id
            """)
    List<String> selectPostNames(@Param("userId") Long userId);

    /** 只取本人账号的登录事件，不依赖日志查询权限 */
    @Select("""
            SELECT DATE_FORMAT(login_time, '%Y-%m-%d %H:%i:%s') AS time, status, msg
            FROM sys_login_log
            WHERE username = #{username}
            ORDER BY login_time DESC, id DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> selectRecentLogins(@Param("username") String username, @Param("limit") int limit);

    /** 资料不涉及权限与凭据，不递增 session_version，修改后当前会话继续有效 */
    @Update("""
            UPDATE sys_user
            SET nickname = #{nickname}, phone = #{phone}, email = #{email}, update_by = #{username}, update_time = NOW()
            WHERE id = #{userId} AND deleted = 0
            """)
    int updateProfile(@Param("userId") Long userId, @Param("username") String username,
                      @Param("nickname") String nickname, @Param("phone") String phone, @Param("email") String email);
}
