package com.banfftech.bean;

import java.util.List;

/**
 * 保存所有审批流程节点
 *
 * @author scy
 * @date 2023/9/11
 */
public class TreeNode {
    private String nodeName;
    private int type;
    private long nodeId;
    private List<List<Condition>> conditionList;
    private NodeUser nodeUserList;
    private String nodeUserText;
    private TreeNode childNode;
    private List<TreeNode> conditionNodes;
    private boolean isdefault; // 是否为默认条件
    private String conditionText;


    public TreeNode(String nodeName, int type, long nodeId, List<List<Condition>> conditionList,
                    NodeUser nodeUserList, TreeNode childNode, List<TreeNode> conditionNodes, boolean isdefault, String conditionText) {
        this.nodeName = nodeName;
        this.type = type;
        this.nodeId = nodeId;
        this.conditionList = conditionList;
        this.nodeUserList = nodeUserList;
        this.childNode = childNode;
        this.conditionNodes = conditionNodes;
        this.isdefault = isdefault;
        this.conditionText = conditionText;
    }

    public TreeNode() {
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    public List<List<Condition>> getConditionList() {
        return conditionList;
    }

    public void setConditionList(List<List<Condition>> conditionList) {
        this.conditionList = conditionList;
    }

    public NodeUser getNodeUserList() {
        return nodeUserList;
    }

    public void setNodeUserList(NodeUser nodeUserList) {
        this.nodeUserList = nodeUserList;
    }

    public TreeNode getChildNode() {
        return childNode;
    }

    public void setChildNode(TreeNode childNode) {
        this.childNode = childNode;
    }

    public List<TreeNode> getConditionNodes() {
        return conditionNodes;
    }

    public void setConditionNodes(List<TreeNode> conditionNodes) {
        this.conditionNodes = conditionNodes;
    }

    public boolean isIsdefault() {
        return isdefault;
    }

    public void setIsdefault(boolean isdefault) {
        this.isdefault = isdefault;
    }

    public String getConditionText() {
        return conditionText;
    }

    public void setConditionText(String conditionText) {
        this.conditionText = conditionText;
    }

    public String getNodeUserText() {
        return nodeUserText;
    }

    public void setNodeUserText(String nodeUserText) {
        this.nodeUserText = nodeUserText;
    }
}

class Condition {
    private String property;
    private String condition;
    private String value;

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

class NodeUser {
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

/**
 * 人工审批选项
 */
class Manual {
    //审批人与提交人相同: self/由提交人自己审批, skip/自动跳过
    private String eqSubmit;
    //多人审批方式: and/会签, or/货签
    private String isArray;
    //无审批人: adopt/自动通过, spare/分配给备用人员或者灌流
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

/**
 * 审批人
 */
class Approver {
    //人员类型: manager/部门负责人, roleTypeId/角色, partyGroup/用户组, party/指定成员, optional/提交人自选, self/提交人本人
    private String type;
    private List<String> value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getValue() {
        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }
}
