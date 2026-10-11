package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DeptWriteRequest {

    @Min(value = 0, message = "关联编号不正确")
    private Long parentId;
    @NotBlank(groups = WriteValidation.Create.class, message = "名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100位")
    private String name;
    @Size(max = 64, message = "负责人长度不能超过64位")
    private String leader;
    @Size(max = 32, message = "联系电话长度不能超过32位")
    @Pattern(regexp = "[0-9+() -]*", message = "联系电话格式不正确")
    private String phone;
    @Size(max = 128, message = "邮箱长度不能超过128位")
    @Email(message = "邮箱格式不正确")
    private String email;
    @Min(value = 0, message = "排序号不能为负数")
    private Integer sort;
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLeader() { return leader; }
    public void setLeader(String leader) { this.leader = leader; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
