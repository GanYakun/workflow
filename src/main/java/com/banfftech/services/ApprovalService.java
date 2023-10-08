package com.banfftech.services;

import com.banfftech.bean.*;
import com.banfftech.common.util.CommonUtils;
import com.banfftech.factory.ExecFactory;
import com.banfftech.factory.ExecNode;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.services.OfbizServiceException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
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

    /**
     * 检查决策
     */
    public static Map<String, Object> checkDecision(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException, OfbizServiceException, OfbizODataException, GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        //节点结束 开始下个节点
        String workEffortId = (String) context.get("workEffortId");
        GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
        Debug.log("当前节点"  + workEffortId + "处理完成, 开始下一个节点");
        TreeNode nextNode = FlowHelper.getNextNode(delegator, workEffort);
        if (nextNode == null) {
            //流程结束 修改业务对象和根节点状态
            String statusId = workEffort.getString("currentStatusId").equals("WEPR_COMPLETE") ? "APPROVAL_APPROVED" : "APPROVAL_REJECTED";
            GenericValue approvalObj = FlowHelper.getApprovalObj(workEffort, delegator);
            FlowHelper.updateEntityStatus(approvalObj, dispatcher, statusId);
            GenericValue topWorkEffort = FlowHelper.getTopWorkEffort(delegator, workEffort.getLong("revisionNumber"));
            topWorkEffort.set("currentStatusId", "WEPR_COMPLETE");
            topWorkEffort.store();
            return resultMap;
        }
        ExecNode execNode = ExecFactory.getExecNode(nextNode.getType());
        if (UtilValidate.isNotEmpty(execNode)) {
            execNode.exec(delegator, nextNode, workEffort);
        }
        return resultMap;
    }

    /**
     * 审批人执行审批,检查会签/或签,是否完成并通过当前节点
     */
    public static Map<String, Object> checkNode(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException, OfbizServiceException {
        Delegator delegator = dctx.getDelegator();
        String workEffortId = (String) context.get("workEffortId");
        String statusId = (String) context.get("statusId");

        GenericValue workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", workEffortId).queryOne();
        TreeNode nodeBeanByWork = FlowHelper.getNodeBeanByWork(delegator, workEffort);
        TreeNode currentNode = FlowHelper.getNodeByNodeId(nodeBeanByWork, workEffort.getLong("priority"));
        NodeUser nodeUserList = currentNode.getNodeUserList();
        Manual manual = nodeUserList.getManual();
        String type = manual.getApprover().getType();
        String isArray = manual.getIsArray();
        if (statusId.equals("WEPR_REFUSE")) {
            //拒绝
            workEffort.set("currentStatusId", "WEPR_REFUSE");
            workEffort.store();
            autoPassAllAssign(delegator, workEffortId, "WEPR_REFUSE");
            return ServiceUtil.returnSuccess();
        }
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
            //如果是或签 需要将其他未审批的节点直接通过
            autoPassAllAssign(delegator, workEffortId, "WEPR_COMPLETE");
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * 将所有未审批的改为通过
     */
    private static void autoPassAllAssign(Delegator delegator, String workEffortId, String statusId) throws GenericEntityException {
        List<GenericValue> otherAssignments = EntityQuery.use(delegator).from("WorkEffortPartyAssignment")
                .where("workEffortId", workEffortId, "statusId", "WEPR_WAIT").queryList();
        for (GenericValue otherAssignment : otherAssignments) {
            otherAssignment.set("statusId", statusId);
            otherAssignment.set("mannerEnumId", "PASS_AUTO");
            otherAssignment.set("thruDate", UtilDateTime.nowTimestamp());
            otherAssignment.store();
        }
    }

}
