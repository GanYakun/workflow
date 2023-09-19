package com.banfftech.services;

import com.banfftech.bean.FlowHelper;
import com.banfftech.bean.TreeNode;
import com.dpbird.odata.services.OfbizServiceException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.Map;

/**
 * @author scy
 * @date 2023/9/11
 */
public class ApprovalService {

    public final static String module = ApprovalService.class.getName();

    public static Map<String, Object> checkNext(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException, OfbizServiceException {
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
        String workEffortId = delegator.getNextSeqId("WorkEffort");
        Debug.log("开始新的审批节点 : " + nodeId + " -> " +  nodeName);
        delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId,
                "workEffortName", nodeName, "workEffortTypeId", "APPROVAL", "currentStatusId", "WEPR_WAIT",
                "priority", nodeId, "workEffortParentId", parentWorkEffort.getString("workEffortId"),
                "revisionNumber", parentWorkEffort.getLong("revisionNumber")));

    }

    private static void routing(Delegator delegator, TreeNode nextNode, GenericValue parentWorkEffort) throws GenericEntityException {
        String nodeName = nextNode.getNodeName();
        long nodeId = nextNode.getNodeId();
        String workEffortId = delegator.getNextSeqId("WorkEffort");
        Debug.log("完成路由节点 : " + nodeId + " -> " +  nodeName);
        Long revisionNumber = parentWorkEffort.getLong("revisionNumber");
        delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId,
                "workEffortName", nodeName, "workEffortTypeId", "ROUTING", "currentStatusId", "WEPR_COMPLETE",
                "priority", nodeId, "workEffortParentId", parentWorkEffort.getString("workEffortId"),
                "revisionNumber", revisionNumber));

        //获取符合条件的分支
        TreeNode conditionNode = FlowHelper.getConditionNode(delegator, nextNode.getConditionNodes(), revisionNumber);
        String condNodeName = conditionNode.getNodeName();
        long condNodeId = conditionNode.getNodeId();
        Debug.log("完成条件节点 : " + nodeId + " -> " + condNodeName);
        String condWorkEffortId = delegator.getNextSeqId("WorkEffort");
        delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", condWorkEffortId,
                "workEffortName", condNodeName, "workEffortTypeId", "CONDITION", "currentStatusId", "WEPR_COMPLETE",
                "priority", condNodeId, "workEffortParentId", workEffortId, "revisionNumber", revisionNumber));

    }

    /**
     * TODO: 处理抄送节点
     */
    private static void makeCopy(Delegator delegator, TreeNode nextNode, GenericValue parentWorkEffort) {
        String nodeName = nextNode.getNodeName();
        long nodeId = nextNode.getNodeId();
        Debug.log("抄送节点 start:" + nodeId);

    }

}
