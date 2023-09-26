package com.banfftech.bean;

import java.util.List;

public class Approver {
    //人员类型: manager/部门负责人, roleTypeId/角色, partyGroup/用户组, party/指定成员, optional/提交人自选, self/提交人本人
    private String type;
    private Object value;
//    private List<String> value;
    private List<String> label;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<String> getLabel() {
        return label;
    }

    public void setLabel(List<String> label) {
        this.label = label;
    }
}