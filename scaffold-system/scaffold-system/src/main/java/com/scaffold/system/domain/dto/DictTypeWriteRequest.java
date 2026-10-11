package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DictTypeWriteRequest {

    @NotBlank(groups = WriteValidation.Create.class, message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称长度不能超过100位")
    private String dictName;
    @NotBlank(groups = WriteValidation.Create.class, message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型长度不能超过100位")
    private String dictType;
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

    public String getDictName() { return dictName; }
    public void setDictName(String dictName) { this.dictName = dictName; }
    public String getDictType() { return dictType; }
    public void setDictType(String dictType) { this.dictType = dictType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
