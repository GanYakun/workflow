package com.banfftech.bean;

/**
 * 人工审批选项
 */
public class Manual {
    //审批人与提交人相同: self/由提交人自己审批, skip/自动跳过
    private String eqSubmit;
    //多人审批方式: and/会签, or/或签
    private String isArray;
    //无审批人: adopt/自动通过, spare/分配给备用人员或者管理员
    private String isEmpty;
    private Approver approver;

    public String getEqSubmit() {
        return eqSubmit;
    }

    public void setEqSubmit(String eqSubmit) {
        this.eqSubmit = eqSubmit;
    }

    public String getIsArray() {
        return isArray;
    }

    public void setIsArray(String isArray) {
        this.isArray = isArray;
    }

    public String getIsEmpty() {
        return isEmpty;
    }

    public void setIsEmpty(String isEmpty) {
        this.isEmpty = isEmpty;
    }

    public Approver getApprover() {
        return approver;
    }

    public void setApprover(Approver approver) {
        this.approver = approver;
    }
}