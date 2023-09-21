package com.banfftech.services;

import com.banfftech.bean.*;
import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.services.OfbizServiceException;
import org.apache.axis2.description.Flow;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.List;
import java.util.Map;

/**
 * @author scy
 * @date 2023/9/11
 */
public class ApprovalService {

    public final static String module = ApprovalService.class.getName();

    public static Map<String, Object> checkDecision(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException, OfbizServiceException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        //有一个节点完成了 开始下个节点
        String workEffortId = (String) context.get("workEffortId");
        GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
        Debug.log("当前节点"  + workEffortId + "处理完成, 开始下一个节点");
        TreeNode nextNode = FlowHelper.getNextNode(delegator, workEffort);
        if (nextNode == null) {
            //TODO: 流程结束
            Debug.log(">>>>>>>>>>>>>>>>>>>>>>>> 流程结束");
            return resultMap;
        }
        //审核节点
        if (nextNode.getType() == 1) {
            approval(delegator, nextNode, workEffort);
        }
        //抄送节点
        if (nextNode.getType() == 2) {
            makeCopy(delegator, nextNode, workEffort);
        }
        //路由节点
        if (nextNode.getType() == 4) {
            routing(delegator, nextNode, workEffort);
        }
        return resultMap;
    }

    private static void approval(Delegator delegator, TreeNode nextNode, GenericValue parentWorkEffort) throws GenericEntityException {
        String nodeName = nextNode.getNodeName();
        long nodeId = nextNode.getNodeId();
        Long revisionNumber = parentWorkEffort.getLong("revisionNumber");
        GenericValue topWorkEffort = FlowHelper.getTopWorkEffort(delegator, revisionNumber);
        GenericValue createParty = CommonUtils.getCreateParty(topWorkEffort);
        NodeUser nodeUserList = nextNode.getNodeUserList();
        String approvalType = nodeUserList.getApprovalType();
        String workEffortId = delegator.getNextSeqId("WorkEffort");
        if (approvalType.equals("adopt") || approvalType.equals("Rejected")) {
            //自动通过或自动拒绝
            String statusId = approvalType.equals("adopt") ? "WEPR_COMPLETE" : "WEPR_REFUSE";
            delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId,
                    "workEffortName", nodeName, "workEffortTypeId", "APPROVAL", "currentStatusId", statusId,
                    "priority", nodeId, "workEffortParentId", parentWorkEffort.getString("workEffortId"),
                    "revisionNumber", revisionNumber,"topWorkEffortId", topWorkEffort.getString("workEffortId")));
            return;
        }
        //审批选项
        Manual manual = nodeUserList.getManual();
        String isEmpty = manual.getIsEmpty();
        //审批人
        Approver approver = manual.getApprover();
        List<String> assiPartyIds = FlowHelper.getApprover(delegator, approver.getType(), approver.getValue(), parentWorkEffort.getLong("revisionNumber"));
        String statusId = "WEPR_WAIT";
        //审批人为空
        if (UtilValidate.isEmpty(assiPartyIds)) {
            if ("adopt".equals(isEmpty)) {
                //自动通过
                statusId = "WEPR_COMPLETE";
            } else {
                //TODO: 备用人员或管理员审批
            }
        }
        //提交人为审批人
        if ("skip".equals(manual.getEqSubmit())) {
            assiPartyIds.remove(createParty.getString("partyId"));
            if (UtilValidate.isEmpty(assiPartyIds)) {
                statusId = "WEPR_COMPLETE";
            }
        }
        delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId,
                "workEffortName", nodeName, "workEffortTypeId", "APPROVAL", "currentStatusId", statusId,
                "priority", nodeId, "workEffortParentId", parentWorkEffort.getString("workEffortId"),
                "revisionNumber", revisionNumber, "topWorkEffortId", topWorkEffort.getString("workEffortId")));
        //分配给人
        for (String assiPartyId : assiPartyIds) {
            delegator.create("WorkEffortPartyAssignment", "workEffortPartyAssignmentId", delegator.getNextSeqId("WorkEffortPartyAssignment"),
                    "workEffortId", workEffortId, "partyId", assiPartyId, "statusId", "WEPR_WAIT");
        }

    }

    private static void routing(Delegator delegator, TreeNode nextNode, GenericValue parentWorkEffort) throws GenericEntityException {
        String nodeName = nextNode.getNodeName();
        long nodeId = nextNode.getNodeId();
        String workEffortId = delegator.getNextSeqId("WorkEffort");
        Debug.log("完成路由节点 : " + nodeId + " -> " +  nodeName);
        Long revisionNumber = parentWorkEffort.getLong("revisionNumber");
        GenericValue topWorkEffort = FlowHelper.getTopWorkEffort(delegator, revisionNumber);
        delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId,
                "workEffortName", nodeName, "workEffortTypeId", "ROUTING", "currentStatusId", "WEPR_COMPLETE",
                "priority", nodeId, "workEffortParentId", parentWorkEffort.getString("workEffortId"),
                "revisionNumber", revisionNumber, "topWorkEffortId", topWorkEffort.getString("workEffortId")));

        //获取符合条件的分支
        TreeNode conditionNode = FlowHelper.getConditionNode(delegator, nextNode.getConditionNodes(), revisionNumber);
        String condNodeName = conditionNode.getNodeName();
        long condNodeId = conditionNode.getNodeId();
        Debug.log("完成条件节点 : " + nodeId + " -> " + condNodeName);
        String condWorkEffortId = delegator.getNextSeqId("WorkEffort");
        delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", condWorkEffortId,
                "workEffortName", condNodeName, "workEffortTypeId", "CONDITION", "currentStatusId", "WEPR_COMPLETE",
                "priority", condNodeId, "workEffortParentId", workEffortId, "revisionNumber", revisionNumber,
                "topWorkEffortId", topWorkEffort.getString("workEffortId")));

    }

    /**
     * TODO: 处理抄送节点
     */
    private static void makeCopy(Delegator delegator, TreeNode nextNode, GenericValue parentWorkEffort) {
        String nodeName = nextNode.getNodeName();
        long nodeId = nextNode.getNodeId();
        Debug.log("抄送节点 start:" + nodeId);

    }


    /**
     * 审批人执行审批,检查会签/或签,是否完成并通过当前节点
     */
    public static Map<String, Object> checkNode(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException, OfbizServiceException {
        Delegator delegator = dctx.getDelegator();
        String workEffortId = (String) context.get("workEffortId");
        GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
        TreeNode nodeBeanByWork = FlowHelper.getNodeBeanByWork(delegator, workEffort);
        TreeNode currentNode = FlowHelper.getNodeByNodeId(nodeBeanByWork, workEffort.getLong("priority"));
        NodeUser nodeUserList = currentNode.getNodeUserList();
        Manual manual = nodeUserList.getManual();
        String type = manual.getApprover().getType();

        String isArray = manual.getIsArray();
        if ("party".equals(type) && "and".equals(isArray)) {
            //会签 全部通过则通过
            long waitApproveCount = EntityQuery.use(delegator).from("WorkEffortPartyAssignment").where("workEffortId", workEffortId, "statusId", "WEPR_WAIT").queryCount();
            if (waitApproveCount == 0) {
                workEffort.set("currentStatusId", "WEPR_COMPLETE");
                workEffort.store();
            }
        } else {
            //其他审批和或签 直接通过
            workEffort.set("currentStatusId", "WEPR_COMPLETE");
            workEffort.store();
        }
        return ServiceUtil.returnSuccess();
    }

}
