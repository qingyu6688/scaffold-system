package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class ConfigWriteRequest {

    @NotBlank(groups = WriteValidation.Create.class, message = "名称不能为空")
    @Size(max = 100, message = "名称长度不能超过100位")
    private String name;
    @NotBlank(groups = WriteValidation.Create.class, message = "参数键名不能为空")
    @Size(max = 100, message = "参数键名长度不能超过100位")
    private String key;
    @NotBlank(groups = WriteValidation.Create.class, message = "参数值不能为空")
    @Size(max = 500, message = "参数值长度不能超过500位")
    private String value;
    @Pattern(regexp = "Y|N", message = "参数类型只能为Y或N")
    private String type;
    private Boolean builtin;
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Boolean getBuiltin() { return builtin; }
    public void setBuiltin(Boolean builtin) { this.builtin = builtin; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
