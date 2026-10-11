package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class UserWriteRequest {

    @NotBlank(groups = WriteValidation.Create.class, message = "登录账号不能为空")
    @Size(max = 64, message = "登录账号长度不能超过64位")
    @Pattern(regexp = "[A-Za-z0-9_.@-]+", message = "登录账号只能包含字母、数字、下划线、点、@或短横线")
    private String username;
    @NotBlank(groups = WriteValidation.Create.class, message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过64位")
    @Size(min = 6, max = 64, message = "密码长度必须在6到64位之间")
    @Pattern(regexp = "[\\x21-\\x7E]+", message = "密码请使用可见的英文字母、数字或符号")
    private String password;
    @NotBlank(groups = WriteValidation.Create.class, message = "用户昵称不能为空")
    @Size(max = 64, message = "用户昵称长度不能超过64位")
    private String nickname;
    @Size(max = 32, message = "联系电话长度不能超过32位")
    @Pattern(regexp = "[0-9+() -]*", message = "联系电话格式不正确")
    private String phone;
    @Size(max = 128, message = "邮箱长度不能超过128位")
    @Email(message = "邮箱格式不正确")
    private String email;
    @NotNull(groups = WriteValidation.Create.class, message = "所属部门不能为空")
    @Min(value = 1, message = "关联编号不正确")
    private Long deptId;
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
    @Size(max = 100, message = "关联项目不能超过100个")
    private List<@NotNull(message = "关联编号不能为空") @Positive(message = "关联编号必须为正数") Long> roleIds;
    @Size(max = 100, message = "关联项目不能超过100个")
    private List<@NotNull(message = "关联编号不能为空") @Positive(message = "关联编号必须为正数") Long> postIds;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public List<Long> getRoleIds() { return roleIds; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
    public List<Long> getPostIds() { return postIds; }
    public void setPostIds(List<Long> postIds) { this.postIds = postIds; }
}
