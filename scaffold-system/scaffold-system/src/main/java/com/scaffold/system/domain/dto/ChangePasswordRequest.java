package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ChangePasswordRequest {

    @NotBlank(message = "不能为空")
    private String oldPassword;

    @NotBlank(message = "不能为空")
    @Size(min = 6, max = 64, message = "长度必须在6到64位之间")
    @Pattern(regexp = "[\\x21-\\x7E]+", message = "密码请使用可见的英文字母、数字或符号")
    private String newPassword;

    public String getOldPassword() { return oldPassword; }
    public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
