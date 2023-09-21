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
