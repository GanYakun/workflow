package com.banfftech.factory;

import com.banfftech.bean.FlowHelper;
import com.banfftech.bean.TreeNode;
import com.dpbird.odata.services.OfbizServiceException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;

/**
 * 处理路由节点
 *
 * @author scy
 * @date 2023/9/22
 */
public class ExecRouting implements ExecNode {

    @Override
    public void exec(Delegator delegator, TreeNode nextNode, GenericValue parentWorkEffort) throws OfbizServiceException {
        try {
            String nodeName = nextNode.getNodeName();
            long nodeId = nextNode.getNodeId();
            String workEffortId = delegator.getNextSeqId("WorkEffort");
            Debug.log("完成路由节点 : " + nodeId + " -> " +  nodeName);
            GenericValue topWorkEffort = FlowHelper.getTopWorkEffort(delegator, parentWorkEffort);
            delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId,
                    "workEffortName", nodeName, "workEffortTypeId", "ROUTING", "currentStatusId", "WEPR_COMPLETE",
                    "priority", nodeId, "workEffortParentId", parentWorkEffort.getString("workEffortId"),
                    "topWorkEffortId", topWorkEffort.getString("workEffortId"), "createdDate", UtilDateTime.nowTimestamp()));
            //查询审批模板和审批对象
            GenericValue templateWorkEffort = topWorkEffort.getRelatedOne("ParentWorkEffort", false);
            GenericValue flowMember = EntityQuery.use(delegator).from("WorkFlowMember")
                    .where("workEffortId", topWorkEffort.getString("workEffortId")).queryFirst();
            ModelEntity modelEntity = delegator.getModelEntity(flowMember.getString("memberEntityName"));
            GenericValue approvalObj = EntityQuery.use(delegator).from(flowMember.getString("memberEntityName"))
                    .where(modelEntity.getFirstPkFieldName(), flowMember.get("memberEntityId")).queryOne();

            //获取符合条件的分支
            TreeNode conditionNode = FlowHelper.getConditionNode(delegator, nextNode.getConditionNodes(), approvalObj, templateWorkEffort);
            String condNodeName = conditionNode.getNodeName();
            long condNodeId = conditionNode.getNodeId();
            Debug.log("完成条件节点 : " + nodeId + " -> " + condNodeName);
            String condWorkEffortId = delegator.getNextSeqId("WorkEffort");
            delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", condWorkEffortId,
                    "workEffortName", condNodeName, "workEffortTypeId", "CONDITION", "currentStatusId", "WEPR_COMPLETE",
                    "priority", condNodeId, "workEffortParentId", workEffortId,
                    "topWorkEffortId", topWorkEffort.getString("workEffortId"), "createdDate", UtilDateTime.nowTimestamp()));
        } catch (GenericEntityException e) {
            throw new OfbizServiceException(e.getMessage());
        }
    }
}
