package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MenuWriteRequest {

    @Min(value = 0, message = "关联编号不正确")
    private Long parentId;
    @NotBlank(groups = WriteValidation.Create.class, message = "名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100位")
    private String name;
    @Size(max = 16, message = "类型长度不能超过16位")
    @Pattern(regexp = "dir|menu|btn", message = "菜单类型不正确")
    private String type;
    @Size(max = 64, message = "图标长度不能超过64位")
    private String icon;
    @Size(max = 200, message = "路由地址长度不能超过200位")
    private String path;
    @Size(max = 200, message = "组件路径长度不能超过200位")
    private String component;
    @Size(max = 128, message = "权限标识长度不能超过128位")
    private String permission;
    @Min(value = 0, message = "排序号不能为负数")
    private Integer sort;
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer visible;
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getVisible() { return visible; }
    public void setVisible(Integer visible) { this.visible = visible; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
