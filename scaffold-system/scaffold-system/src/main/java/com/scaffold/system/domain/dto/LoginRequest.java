package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.*;

public class LoginRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "账号长度不能超过64位")
    @Pattern(regexp = "[A-Za-z0-9_.@-]+", message = "登录账号格式不正确")
    private String username;

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "密码长度不能超过64位")
    private String password;

    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 4, message = "验证码必须为4位")
    private String captcha;

    @NotBlank(message = "验证码标识不能为空")
    @Pattern(regexp = "[a-f0-9]{32}", message = "验证码标识不正确")
    private String captchaUuid;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCaptcha() { return captcha; }
    public void setCaptcha(String captcha) { this.captcha = captcha; }
    public String getCaptchaUuid() { return captchaUuid; }
    public void setCaptchaUuid(String captchaUuid) { this.captchaUuid = captchaUuid; }
}
