package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.*;

public class UserStatusRequest {

    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    @NotNull(message = "用户状态不能为空")
    private Integer status;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
