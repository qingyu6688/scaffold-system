package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RoleWriteRequest {

    @NotBlank(groups = WriteValidation.Create.class, message = "名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100位")
    private String name;
    @NotBlank(groups = WriteValidation.Create.class, message = "编码不能为空")
    @Size(max = 64, message = "编码长度不能超过64位")
    private String code;
    @Size(max = 32, message = "数据权限范围长度不能超过32位")
    @Pattern(regexp = "ALL|DEPT_AND_CHILD|DEPT|CUSTOM|SELF", message = "数据范围不正确")
    private String dataScope;
    @Min(value = 0, message = "排序号不能为负数")
    private Integer sort;
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;
    @Size(max = 100, message = "关联项目不能超过100个")
    private List<@NotNull(message = "关联编号不能为空") @Positive(message = "关联编号必须为正数") Long> menuIds;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDataScope() { return dataScope; }
    public void setDataScope(String dataScope) { this.dataScope = dataScope; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<Long> getMenuIds() { return menuIds; }
    public void setMenuIds(List<Long> menuIds) { this.menuIds = menuIds; }
}
