package com.banfftech.factory;

import com.banfftech.bean.*;
import com.banfftech.common.util.CommonUtils;
import com.dpbird.odata.services.OfbizServiceException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理审批节点
 *
 * @author scy
 * @date 2023/9/22
 */
public class ExecApproval implements ExecNode {

    @Override
    public void exec(Delegator delegator, TreeNode nextNode, GenericValue parentWorkEffort) throws OfbizServiceException {
        try {
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
                        "revisionNumber", revisionNumber, "topWorkEffortId", topWorkEffort.getString("workEffortId"), "createdDate", UtilDateTime.nowTimestamp()));
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
                    //组织管理员审批
                    assiPartyIds = UtilMisc.toList("org_admin");
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
                    "revisionNumber", revisionNumber, "topWorkEffortId", topWorkEffort.getString("workEffortId"), "createdDate", UtilDateTime.nowTimestamp()));
            //分配给人
            for (String assiPartyId : assiPartyIds) {
                delegator.create("WorkEffortPartyAssignment", "workEffortPartyAssignmentId", delegator.getNextSeqId("WorkEffortPartyAssignment"),
                        "workEffortId", workEffortId, "partyId", assiPartyId, "statusId", "WEPR_WAIT", "fromDate", UtilDateTime.nowTimestamp());
            }
        } catch (GenericEntityException e) {
            throw new OfbizServiceException(e.getMessage());
        }
    }
}
