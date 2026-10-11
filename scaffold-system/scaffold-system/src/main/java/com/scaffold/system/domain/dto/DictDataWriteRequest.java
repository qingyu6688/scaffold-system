package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DictDataWriteRequest {

    @NotBlank(groups = WriteValidation.Create.class, message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型长度不能超过100位")
    private String dictType;
    @NotBlank(groups = WriteValidation.Create.class, message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签长度不能超过100位")
    private String label;
    @NotBlank(groups = WriteValidation.Create.class, message = "参数值不能为空")
    @Size(max = 100, message = "参数值长度不能超过100位")
    private String value;
    @Min(value = 0, message = "排序号不能为负数")
    private Integer sort;
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
    @Size(max = 32, message = "展示样式长度不能超过32位")
    private String tone;
    private Boolean def;
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

    public String getDictType() { return dictType; }
    public void setDictType(String dictType) { this.dictType = dictType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    public Boolean getDef() { return def; }
    public void setDef(Boolean def) { this.def = def; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
