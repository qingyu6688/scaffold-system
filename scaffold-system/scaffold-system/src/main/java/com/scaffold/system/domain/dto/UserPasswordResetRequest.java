package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserPasswordResetRequest {

    @Size(min = 6, max = 64, message = "密码长度必须在6到64位之间")
    @Pattern(regexp = "[\\x21-\\x7E]+", message = "密码请使用可见的英文字母、数字或符号")
    @NotBlank(message = "新密码不能为空")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
