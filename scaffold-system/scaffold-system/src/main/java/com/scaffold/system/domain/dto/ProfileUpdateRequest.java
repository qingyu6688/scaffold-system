package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 个人资料只开放姓名与联系方式；账号、部门、角色等仍由管理员维护。 */
public class ProfileUpdateRequest {

    @NotBlank(message = "用户昵称不能为空")
    @Size(max = 64, message = "用户昵称长度不能超过64位")
    private String nickname;
    @Size(max = 32, message = "联系电话长度不能超过32位")
    @Pattern(regexp = "[0-9+() -]*", message = "联系电话格式不正确")
    private String phone;
    @Size(max = 128, message = "邮箱长度不能超过128位")
    @Email(message = "邮箱格式不正确")
    private String email;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
