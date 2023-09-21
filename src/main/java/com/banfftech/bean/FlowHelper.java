package com.banfftech.bean;

import com.dpbird.odata.services.OfbizServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityComparisonOperator;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批流工具类
 *
 * @author scy
 * @date 2023/9/12
 */
public class FlowHelper {
    /**
     * 保存所有的审批结构
     */
    private static final Map<String, TreeNode> FLOW_TREES = new HashMap<>();

    /**
     * 运算符映射
     */
    public final static Map<String, EntityComparisonOperator<?, ?>> OPERATOR_MAP = new HashMap<>();

    static {
        OPERATOR_MAP.put("eq", EntityOperator.EQUALS);
        OPERATOR_MAP.put("ne", EntityOperator.NOT_EQUAL);
        OPERATOR_MAP.put("ge", EntityOperator.GREATER_THAN_EQUAL_TO);
        OPERATOR_MAP.put("gt", EntityOperator.GREATER_THAN);
        OPERATOR_MAP.put("le", EntityOperator.LESS_THAN_EQUAL_TO);
        OPERATOR_MAP.put("lt", EntityOperator.LESS_THAN);
        OPERATOR_MAP.put("in", EntityOperator.EQUALS);
        OPERATOR_MAP.put("contains", EntityOperator.LIKE);
    }


    /**
     * 根据任意节点 获取结构对象
     */
    public static TreeNode getNodeBeanByWork(Delegator delegator, GenericValue currWorkEffort) throws OfbizServiceException {
        try {
            //查询根节点
            GenericValue rootWorkEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortTypeId", "ROOT_NODE",
                    "revisionNumber", currWorkEffort.getLong("revisionNumber")).queryFirst();
            //根节点的父级 就是流程模板
            GenericValue tempWorkEffort = rootWorkEffort.getRelatedOne("ParentWorkEffort", false);
            String noteId = tempWorkEffort.getString("noteId");
            TreeNode treeNode = FLOW_TREES.get(noteId);
            if (UtilValidate.isEmpty(treeNode)) {
                GenericValue noteData = tempWorkEffort.getRelatedOne("NoteData", false);
                treeNode = new ObjectMapper().readValue(noteData.getString("noteInfo"), TreeNode.class);
                FLOW_TREES.put(noteId, treeNode);
                return treeNode;
            }
            return treeNode;
        } catch (JsonProcessingException | GenericEntityException e) {
            throw new OfbizServiceException("格式化审批节点错误: " + e.getMessage());
        }
    }

    /**
     * 获取子节点 如果当前是某个路由的最后一个节点 要获取对应的路由的下一个节点
     */
    public static TreeNode getNextNode(Delegator delegator, GenericValue workEffort) throws OfbizServiceException, GenericEntityException {
        TreeNode nodeBeanByWork = getNodeBeanByWork(delegator, workEffort);
        TreeNode node = getNodeByNodeId(nodeBeanByWork, workEffort.getLong("priority"));
        TreeNode childNode = node.getChildNode();
        if (UtilValidate.isNotEmpty(childNode)) {
            return childNode;
        }
        //获取路由的子节点
        return getRoutNextNode(delegator, workEffort);
    }

    /**
     * 获取节点的路由子节点
     */
    public static TreeNode getRoutNextNode(Delegator delegator, GenericValue workEffort) throws OfbizServiceException, GenericEntityException {
        Long revisionNumber = workEffort.getLong("revisionNumber");
        TreeNode routNode = getRoutNode(delegator, workEffort);
        if (UtilValidate.isEmpty(routNode)) {
            return null;
        }
        GenericValue routWorkEffort = EntityQuery.use(delegator).from("WorkEffort").where("priority", routNode.getNodeId(), "revisionNumber", revisionNumber).queryFirst();
        TreeNode routChildNode = routNode.getChildNode();
        if (UtilValidate.isNotEmpty(routChildNode)) {
            //返回路由的子节点
            long childNodeId = routChildNode.getNodeId();
            GenericValue childWorkEffort = EntityQuery.use(delegator).from("WorkEffort").where("priority", childNodeId, "revisionNumber", revisionNumber).queryFirst();
            //如果子节点是完成状态 查询父级路由子节点
            if (UtilValidate.isNotEmpty(childWorkEffort) && childWorkEffort.getString("currentStatusId").equals("WEPR_COMPLETE")) {
                return getRoutNextNode(delegator, routWorkEffort);
            }
            return routChildNode;
        } else {
            //路由没有子节点 查询父级路由的子节点
            return getRoutNextNode(delegator, routWorkEffort);
        }
    }

    /**
     * 获取节点归属的路由节点
     */
    public static TreeNode getRoutNode(Delegator delegator, GenericValue workEffort) throws OfbizServiceException, GenericEntityException {
        GenericValue parentWorkEffort = getParentWorkEffort(workEffort, UtilMisc.toMap("workEffortTypeId", "ROUTING"));
        if (UtilValidate.isEmpty(parentWorkEffort)) {
            return null;
        }
        TreeNode nodeBeanByWork = getNodeBeanByWork(delegator, parentWorkEffort);
        return getNodeByNodeId(nodeBeanByWork, parentWorkEffort.getLong("priority"));
    }

    /**
     * 查询根节点
     */
    public static GenericValue getTopWorkEffort(Delegator delegator, Long revisionNumber) throws GenericEntityException {
        return EntityQuery.use(delegator).from("WorkEffort").where("workEffortTypeId", "ROOT_NODE",
                "revisionNumber", revisionNumber).queryFirst();
    }

    /**
     * 根据条件获取parent workEffort
     */
    public static GenericValue getParentWorkEffort(GenericValue workEffort, Map<String, Object> byAnd) throws GenericEntityException {
        GenericValue parentWorkEffort = workEffort.getRelatedOne("ParentWorkEffort", false);
        if (UtilValidate.isEmpty(parentWorkEffort)) {
            return null;
        }
        List<GenericValue> results = EntityUtil.filterByAnd(UtilMisc.toList(parentWorkEffort), byAnd);
        if (UtilValidate.isEmpty(results)) {
            //不符合条件的父级 继续查询
            return getParentWorkEffort(parentWorkEffort, byAnd);
        } else {
            return parentWorkEffort;
        }
    }

    /**
     * 根据节点id获取节点
     */
    public static TreeNode getNodeByNodeId(TreeNode rootNode, long targetNodeId) {
        if (rootNode == null) {
            return null;
        }
        if (rootNode.getNodeId() == targetNodeId) {
            return rootNode;
        }
        // 递归查找子节点
        if (rootNode.getChildNode() != null) {
            TreeNode childNode = getNodeByNodeId(rootNode.getChildNode(), targetNodeId);
            if (childNode != null) {
                return childNode;
            }
        }
        // 递归查找条件节点
        if (rootNode.getConditionNodes() != null) {
            for (TreeNode conditionNode : rootNode.getConditionNodes()) {
                TreeNode foundNode = getNodeByNodeId(conditionNode, targetNodeId);
                if (foundNode != null) {
                    return foundNode;
                }
            }
        }
        return null; // 未找到匹配的节点
    }

    /**
     * 解析条件表达式
     *
     * @param conditionNodes 所有的条件节点
     * @param revisionNumber 审批流编号
     * @return 从所有的条件节点获取一个符合条件的节点返回 如果全都不符合返回默认节点
     */
    public static TreeNode getConditionNode(Delegator delegator, List<TreeNode> conditionNodes, Long revisionNumber) throws GenericEntityException {
        //查询审批主流程
        GenericValue rootWorkEffort = EntityQuery.use(delegator).from("WorkEffort")
                .where("workEffortTypeId", "ROOT_NODE", "revisionNumber", revisionNumber).queryFirst();
        GenericValue mainProcess = EntityQuery.use(delegator).from("MainProcess")
                .where("workFlowId", rootWorkEffort.getString("workEffortParentId")).queryFirst();
        GenericValue processEntity = mainProcess.getRelatedOne("ProcessEntity", false);
        List<GenericValue> processFields = processEntity.getRelated("ProcessField", null, null, false);
        //查询审批对象数据
        GenericValue flowMember = EntityQuery.use(delegator).from("WorkFlowMember").where("workEffortId", rootWorkEffort.getString("workEffortId")).queryFirst();
        ModelEntity modelEntity = delegator.getModelEntity(flowMember.getString("memberEntityName"));
        GenericValue approvalObj = EntityQuery.use(delegator).from(flowMember.getString("memberEntityName")).where(modelEntity.getFirstPkFieldName(), flowMember.get("memberEntityId")).queryOne();


        for (TreeNode conditionNode : conditionNodes) {
            if (conditionNode.isIsdefault()) {
                continue;
            }
            //一个节点所有的条件组
            List<List<Condition>> conditionGroups = conditionNode.getConditionList();
            //条件组之间的关系是or 有一个匹配就成功
            for (List<Condition> conditionsGro : conditionGroups) {
                boolean result = checkConditionGroup(delegator, conditionsGro, approvalObj, processFields);
                if (result) {
                    return conditionNode;
                }
            }
        }
        return conditionNodes.stream().filter(TreeNode::isIsdefault).findFirst().orElse(null);
    }

    private static boolean checkConditionGroup(Delegator delegator, List<Condition> conditions,
                                               GenericValue genericValue, List<GenericValue> processFields) throws GenericEntityException {
        //条件组之间的关系的and 有一个失败就失败
        for (Condition condition : conditions) {
            if (!checkCondition(delegator, condition, genericValue, processFields)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析条件表达式
     */
    private static boolean checkCondition(Delegator delegator, Condition condition, GenericValue genericValue, List<GenericValue> processFields) throws GenericEntityException {
        //表达式
        String property = condition.getProperty();
        String operator = condition.getCondition();
        String value = condition.getValue();
        GenericValue processField = EntityUtil.getFirst(EntityUtil.filterByAnd(processFields, UtilMisc.toMap("processFieldName", property)));
        if (UtilValidate.isEmpty(processField)) {
            return false;
        }
        String fieldTypeId = processField.getString("processFieldTypeId");
        //如果是人员需要做特殊的匹配 不能直接用运算符判断
        if (fieldTypeId.equals("PARTY")) {
            GenericValue party = EntityQuery.use(delegator).from("Party").where("partyId", value).queryOne();
            if (party.getString("partyTypeId").equals("PARTY_GROUP")) {
                //匹配部门
                String propertyValue = genericValue.getString(property);
                GenericValue department = EntityQuery.use(delegator).from("PartyRelationship").where("roleTypeIdFrom", "DEPARTMENT",
                        "partyIdFrom", value, "partyIdTo", propertyValue).queryFirst();
                return UtilValidate.isNotEmpty(department);
            }
        }
        //使用condition匹配
        EntityCondition entityCondition = EntityCondition.makeCondition(property, OPERATOR_MAP.get(operator), value);
        List<GenericValue> genericValues = EntityUtil.filterByCondition(UtilMisc.toList(genericValue), entityCondition);
        return UtilValidate.isNotEmpty(genericValues);
    }

    /**
     * 获取需要分配的审批人
     *
     * @param approverType 审批类型  manager/部门负责人, roleTypeId/角色, partyGroup/用户组, party/指定成员, optional/提交人自选, self/提交人本人
     * @param revisionNumber 审批流程编号
     * @return 返回所有审批人的partyId
     */
    public static List<String> getApprover(Delegator delegator, String approverType, List<String> approverValue, Long revisionNumber) throws GenericEntityException {
        GenericValue rootWorkEffort = EntityQuery.use(delegator).from("WorkEffort")
                .where("workEffortTypeId", "ROOT_NODE", "revisionNumber", revisionNumber).queryFirst();
        //发起人
        String createdByUserLogin = rootWorkEffort.getString("createdByUserLogin");
        List<String> assignmentPartyIds = new ArrayList<>();
        if ("manager".equals(approverType)) {
            //查询提交人的部门负责人
        }
        if ("roleTypeId".equals(approverType)) {
            //查询角色人员
        }
        if ("partyGroup".equals(approverType)) {
            //查询用户组人员
        }
        if ("party".equals(approverType)) {
            //指定成员
            return approverValue;
        }
        if ("optional".equals(approverType)) {
            //提交人自选
        }
        if ("self".equals(approverType)) {
            //提交人本人
        }
        return assignmentPartyIds;
    }

}
