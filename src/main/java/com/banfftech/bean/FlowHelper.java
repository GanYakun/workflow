package com.banfftech.bean;

import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.Util;
import com.dpbird.odata.edm.OfbizCsdlEntityType;
import com.dpbird.odata.services.OfbizServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.ofbiz.base.conversion.ConversionException;
import org.apache.ofbiz.base.conversion.JSONConverters;
import org.apache.ofbiz.base.lang.JSON;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.*;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

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
        if (workEffort.getString("currentStatusId").equals("WEPR_REFUSE")) {
            //不通过
            return null;
        }
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
     * @param approvalObj 审批对象
     * @param templateWorkEffort 审批模板
     * @return 从所有的条件节点获取一个符合条件的节点返回 如果全都不符合返回默认节点
     */
    public static TreeNode getConditionNode(Delegator delegator, List<TreeNode> conditionNodes, GenericValue approvalObj,
                                            GenericValue templateWorkEffort) throws GenericEntityException {
        //查询审批主流程
        GenericValue mainProcess = EntityQuery.use(delegator).from("MainProcess")
                .where("workFlowId", templateWorkEffort.getString("workEffortId")).queryFirst();
        GenericValue processEntity = mainProcess.getRelatedOne("ProcessEntity", false);
        List<GenericValue> processFields = processEntity.getRelated("ProcessField", null, null, false);
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
        //条件之间的关系是and 有一个失败就失败
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
        Object value = condition.getValue();
        GenericValue processField = EntityUtil.getFirst(EntityUtil.filterByAnd(processFields, UtilMisc.toMap("processFieldName", property)));
        if (UtilValidate.isEmpty(processField)) {
            return false;
        }
        String fieldTypeId = processField.getString("processFieldTypeId");
        String propertyValue = genericValue.getString(property);
        //如果是人员需要做特殊的匹配 不能直接用运算符判断
        if ("PARTY".equals(fieldTypeId)) {
            List<String> types = UtilGenerics.checkList(condition.getType());
            List<String> values = UtilGenerics.checkList(condition.getValue());
            Set<String> parties = getParties(delegator, types, values);
            if ("in".equals(operator)) {
                return parties.contains(propertyValue);
            } else {
                return !parties.contains(propertyValue);
            }
        }
        Object valueObj = value;
        //转换日期
        if (fieldTypeId.equals("DATE")) {
            valueObj = Util.getSqlDate(value.toString());
        }
        if (fieldTypeId.equals("DATE_TIME")) {
            valueObj = Util.getSqlTimestamp(value.toString());
        }
        if (fieldTypeId.equals("NUMBER")) {
            valueObj = new BigDecimal(value.toString());
        }
        //使用condition匹配
        EntityCondition entityCondition = EntityCondition.makeCondition(property, OPERATOR_MAP.get(operator), valueObj);
        List<GenericValue> genericValues = EntityUtil.filterByCondition(UtilMisc.toList(genericValue), entityCondition);
        return UtilValidate.isNotEmpty(genericValues);
    }

    /**
     * 根据范围获取人员
     */
    public static Set<String> getParties(Delegator delegator, List<String> types, List<String> IdList) throws GenericEntityException {
        Set<String> partyIds = new HashSet<>();
        List<String> userGroupIds = new ArrayList<>();
        List<String> departmentIds = new ArrayList<>();
        List<String> roleTypeIds = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            String partyType = types.get(i);
            String id = IdList.get(i);
            if ("Party".equals(partyType)) {
                partyIds.add(id);
            }
            if ("PartyGroup".equals(partyType)) {
                departmentIds.add(id);
            }
            if ("Role".equals(partyType)) {
                roleTypeIds.add(id);
            }
            if ("UserGroup".equals(partyType)) {
                userGroupIds.add(id);
            }
        }
        if (UtilValidate.isNotEmpty(userGroupIds)) {
            List<String> resultIds = EntityQuery.use(delegator).from("PartyRelationship")
                    .where(EntityCondition.makeCondition("partyIdFrom", EntityOperator.IN, userGroupIds)).getFieldList("partyIdTo");
            partyIds.addAll(resultIds);
        }
        if (UtilValidate.isNotEmpty(departmentIds)) {
            EntityCondition condition = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("partyIdFrom", EntityOperator.IN, departmentIds),
                    EntityCondition.makeCondition("roleTypeIdTo", "ORD_EMPLOYEE")));
            List<String> resultIds = EntityQuery.use(delegator).from("PartyRelationship").where(condition).getFieldList("partyIdTo");
            partyIds.addAll(resultIds);
        }
        if (UtilValidate.isNotEmpty(roleTypeIds)) {
            List<String> resultIds = EntityQuery.use(delegator).from("PartyRole")
                    .where(EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, roleTypeIds)).getFieldList("partyId");
            partyIds.addAll(resultIds);
        }
        return partyIds;
    }


    /**
     * 获取需要分配的审批人
     *
     * @param approverType 审批类型  manager/部门负责人, roleTypeId/角色, partyGroup/用户组, party/指定成员, optional/提交人自选, self/提交人本人
     * @param revisionNumber 审批流程编号
     * @return 返回所有审批人的partyId
     */
    public static List<String> getApprover(Delegator delegator, String approverType, Object approverValue, Long revisionNumber, long nodeId) throws OfbizServiceException {
        List<String> assignmentPartyIds = new ArrayList<>();
        try {
            GenericValue rootWorkEffort = EntityQuery.use(delegator).from("WorkEffort")
                    .where("workEffortTypeId", "ROOT_NODE", "revisionNumber", revisionNumber).queryFirst();
            //发起人
            String createdByUserLogin = rootWorkEffort.getString("createdByUserLogin");
            GenericValue createUser = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", createdByUserLogin), false);
            if ("manager".equals(approverType)) {
                //查询提交人的部门负责人
                GenericValue manager = EntityQuery.use(delegator).from("PartyRelationship")
                        .where("partyIdTo", createUser.getString("partyId"), "roleTypeIdTo", "MANAGER").queryFirst();
                if (UtilValidate.isNotEmpty(manager)) {
                    assignmentPartyIds.add(manager.getString("partyIdFrom"));
                }
                return assignmentPartyIds;
            }
            if ("roleTypeId".equals(approverType)) {
                //查询角色人员
                List<String> partyIds = EntityQuery.use(delegator).from("PartyRole")
                        .where(EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, approverValue)).getFieldList("partyId");
                assignmentPartyIds.addAll(partyIds);
            }
            if ("partyGroup".equals(approverType)) {
                //查询用户组人员
                List<String> partyIds = EntityQuery.use(delegator).from("PartyRelationship")
                        .where(EntityCondition.makeCondition("partyIdFrom", EntityOperator.IN, approverValue)).getFieldList("partyIdTo");
                assignmentPartyIds.addAll(partyIds);
            }
            if ("party".equals(approverType)) {
                //指定成员
                return UtilGenerics.checkList(approverValue);
            }
            if ("self".equals(approverType)) {
                //提交人本人
                assignmentPartyIds.add(createUser.getString("partyId"));
            }
            if ("optional".equals(approverType)) {
                //用户自选
                GenericValue runtimeData = rootWorkEffort.getRelatedOne("RuntimeData", false);
                if (UtilValidate.isNotEmpty(runtimeData.getString("runtimeInfo"))) {
                    String runtimeInfo = runtimeData.getString("runtimeInfo");
                    JSONArray jsonArray = JSONArray.fromObject(runtimeInfo);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject approver = jsonArray.getJSONObject(i);
                        if (approver.getLong("nodeId") == nodeId) {
                            assignmentPartyIds.add(approver.getString("partyId"));
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            throw new OfbizServiceException(e.getMessage());
        }
        return assignmentPartyIds;
    }

    /**
     * 刷新缓存
     */
    public static void flushTreeNodeCache(GenericValue noteData) throws OfbizODataException {
        try {
            TreeNode treeNode = new ObjectMapper().readValue(noteData.getString("noteInfo"), TreeNode.class);
            FLOW_TREES.put(noteData.getString("noteId"), treeNode);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new OfbizODataException(e.getMessage());
        }
    }

    /**
     * 获取业务对象
     */
    public static GenericValue getApprovalObj(GenericValue workEffort,  Delegator delegator) throws GenericEntityException {
        //查询审批主流程
        GenericValue rootWorkEffort = EntityQuery.use(delegator).from("WorkEffort")
                .where("workEffortTypeId", "ROOT_NODE", "revisionNumber", workEffort.getLong("revisionNumber")).queryFirst();
        //查询审批对象数据
        GenericValue flowMember = EntityQuery.use(delegator).from("WorkFlowMember").where("workEffortId", rootWorkEffort.getString("workEffortId")).queryFirst();
        ModelEntity modelEntity = delegator.getModelEntity(flowMember.getString("memberEntityName"));
        return EntityQuery.use(delegator).from(modelEntity.getEntityName()).where(modelEntity.getFirstPkFieldName(), flowMember.get("memberEntityId")).queryOne();

    }

    /**
     * 更新审批对象的状态
     */
    public static void updateEntityStatus(GenericValue genericValue, LocalDispatcher dispatcher, String statusId)
            throws OfbizODataException, GenericEntityException, GenericServiceException {
        Delegator delegator = dispatcher.getDelegator();
        GenericValue systemUser = Util.getSystemUser(delegator);
        String updateService = Util.getEntityActionService(null, genericValue.getEntityName(), "update", delegator);
        HashMap<String, Object> serviceParam = new HashMap<>(genericValue.getPrimaryKey());
        serviceParam.put("statusId", statusId);
        serviceParam.put("userLogin", systemUser);
        dispatcher.runSync(updateService, serviceParam);
    }

    /**
     * 根据实体和类型获取模板WorkEffort
     */
    public static GenericValue getTemplateWorkEffort(Delegator delegator, String entityName, String type) throws OfbizODataException, GenericEntityException {
        GenericValue processEntity = EntityQuery.use(delegator).from("ProcessEntity")
                .where("processEntityName", entityName, "processEntityTypeId", type).queryFirst();
        if (UtilValidate.isEmpty(processEntity)) {
            throw new OfbizODataException("业务对象不存在: " + entityName);
        }
        //获取主流程对象
        GenericValue mainProcess = EntityQuery.use(delegator).from("MainProcess")
                .where(UtilMisc.toMap("statusId", "PROCESS_ENABLED", "processEntityId", processEntity.getString("processEntityId"))).queryFirst();
        if (UtilValidate.isEmpty(mainProcess) || UtilValidate.isEmpty(mainProcess.getString("workFlowId"))) {
            throw new OfbizODataException("未找到有效的审批流程");
        }
        return mainProcess.getRelatedOne("WorkEffort", false);
    }

    /**
     * 获取所有需要用户自选的节点
     */
    public static List<TreeNode> getCustomerDefNodes(Delegator delegator, GenericValue approvalObj, String typeId)
            throws OfbizODataException, GenericEntityException, JsonProcessingException {
        GenericValue templateWorkEffort = getTemplateWorkEffort(delegator, approvalObj.getEntityName(), typeId);
        GenericValue noteData = templateWorkEffort.getRelatedOne("NoteData", false);
        TreeNode treeNode = FLOW_TREES.get(noteData.getString("noteId"));
        if (UtilValidate.isEmpty(treeNode)) {
            treeNode = new ObjectMapper().readValue(noteData.getString("noteInfo"), TreeNode.class);
            FLOW_TREES.put(noteData.getString("noteId"), treeNode);
        }
        return getCustomerDefNodes(treeNode, new ArrayList<>(), approvalObj, templateWorkEffort);
    }

    /**
     * 获取所有需要用户自选的节点
     */
    public static List<TreeNode> getCustomerDefNodes(TreeNode treeNode, List<TreeNode> result, GenericValue approvalObj,
                                                     GenericValue templateWorkEffort) throws GenericEntityException {
        if (UtilValidate.isEmpty(treeNode)) {
            return result;
        }
        if (treeNode.getType() == 1) {
            NodeUser nodeUserList = treeNode.getNodeUserList();
            if ("manual".equals(nodeUserList.getApprovalType())) {
                Manual manual = nodeUserList.getManual();
                String type = manual.getApprover().getType();
                if ("optional".equals(type)) {
                    result.add(treeNode);
                }
            }
        }
        getCustomerDefNodes(treeNode.getChildNode(), result, approvalObj, templateWorkEffort);
        List<TreeNode> conditionNodes = treeNode.getConditionNodes();
        if (UtilValidate.isNotEmpty(conditionNodes)) {
            TreeNode conditionNode = getConditionNode(approvalObj.getDelegator(), conditionNodes, approvalObj, templateWorkEffort);
            getCustomerDefNodes(conditionNode.getChildNode(), result, approvalObj, templateWorkEffort);
        }
        return result;

    }


}
