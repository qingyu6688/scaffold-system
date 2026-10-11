package com.scaffold.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PostWriteRequest {

    @NotBlank(groups = WriteValidation.Create.class, message = "岗位编码不能为空")
    @Size(max = 64, message = "岗位编码长度不能超过64位")
    private String postCode;
    @NotBlank(groups = WriteValidation.Create.class, message = "岗位名称不能为空")
    @Size(max = 100, message = "岗位名称长度不能超过100位")
    private String postName;
    @Min(value = 0, message = "排序号不能为负数")
    private Integer sort;
    @Min(value = 0, message = "状态只能为0或1")
    @Max(value = 1, message = "状态只能为0或1")
    private Integer status;
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
