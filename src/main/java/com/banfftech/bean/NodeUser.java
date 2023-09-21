package com.banfftech.bean;

public class NodeUser {
    //审批类型: manual/人工审批, adopt/自动通过, Rejected/自动拒绝
    private String approvalType;
    private Manual manual;

    public String getApprovalType() {
        return approvalType;
    }

    public void setApprovalType(String approvalType) {
        this.approvalType = approvalType;
    }

    public Manual getManual() {
        return manual;
    }

    public void setManual(Manual manual) {
        this.manual = manual;
    }
}