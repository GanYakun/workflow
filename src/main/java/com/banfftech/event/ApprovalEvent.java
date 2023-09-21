package com.banfftech.event;

import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.Util;
import com.dpbird.odata.edm.OdataOfbizEntity;
import net.sf.json.JSONObject;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import java.util.Map;

/**
 * 处理审批流相关代码
 *
 * @author scy
 * @date 2023/9/11
 */
public class ApprovalEvent {

    /**
     * 流程设计发布,客户端会传递完整的流程json,保存它
     */
    public static void saveProcess(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                   EdmBindingTarget edmBindingTarget) throws GenericEntityException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.get("mainProcess");
        //所有的节点数据
        String nodeData = (String) actionParameters.get("nodeData");
        GenericValue process = EntityQuery.use(delegator).from("MainProcess").where(ofbizEntity.getGenericValue().getPrimaryKey()).queryOne();
        String workFlowId = process.getString("workFlowId");
        if (UtilValidate.isEmpty(workFlowId)) {
            //创建流程
            String noteId = delegator.getNextSeqId("NoteData");
            delegator.create("NoteData", UtilMisc.toMap("noteId", noteId, "noteName", "FLOW_JSON", "noteInfo", nodeData, "noteDateTime", UtilDateTime.nowTimestamp()));

            String workEffortId = delegator.getNextSeqId("WorkEffort");
            delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId, "workEffortTypeId", "WORK_FLOW", "noteId", noteId));
            process.set("workFlowId", workEffortId);
            process.store();
        } else {
            //更新流程
            GenericValue workEffort = process.getRelatedOne("WorkEffort", false);
            GenericValue noteData = workEffort.getRelatedOne("NoteData", false);
            noteData.set("noteInfo", nodeData);
            noteData.set("noteDateTime", UtilDateTime.nowTimestamp());
            noteData.store();
        }
    }

    /**
     * 发布
     */
    public static void publishFlow(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                   EdmBindingTarget edmBindingTarget) throws GenericEntityException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.get("mainProcess");
        delegator.storeByCondition("MainProcess", UtilMisc.toMap("statusId", "PROCESS_ENABLED"),
                EntityCondition.makeCondition(ofbizEntity.getGenericValue().getPrimaryKey()));
    }

    /**
     * 停用
     */
    public static void stopFlow(Map<String, Object> oDataContext, Map<String, Object> actionParameters,
                                   EdmBindingTarget edmBindingTarget) throws GenericEntityException, GenericServiceException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.get("mainProcess");
        delegator.storeByCondition("MainProcess", UtilMisc.toMap("statusId", "PROCESS_NOT_ENABLED"),
                EntityCondition.makeCondition(ofbizEntity.getGenericValue().getPrimaryKey()));
    }

    /**
     * 提交审批
     */
    public static void submitApproval(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException, OfbizODataException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.values().iterator().next();
        String typeId = (String) actionParameters.get("type");
        GenericValue genericValue = ofbizEntity.getGenericValue();
        String entityName = genericValue.getEntityName();
//        String typeId = genericValue.getString(Util.firstLowerCase(entityName) + "TypeId");
        GenericValue processEntity = EntityQuery.use(delegator).from("ProcessEntity")
                .where("processEntityName", entityName, "processEntityTypeId", typeId).queryFirst();
        if (UtilValidate.isEmpty(processEntity)) {
            throw new OfbizODataException("业务对象不存在: " + entityName);
        }
        //获取主流程对象
        GenericValue mainProcess = EntityQuery.use(delegator).from("MainProcess").where(processEntity.getPrimaryKey()).queryFirst();
        if (UtilValidate.isEmpty(mainProcess) || UtilValidate.isEmpty(mainProcess.getString("workFlowId"))) {
            throw new OfbizODataException("未配置审批流程");
        }
        GenericValue templateWorkEffort = mainProcess.getRelatedOne("WorkEffort", false);
        GenericValue noteData = templateWorkEffort.getRelatedOne("NoteData", false);
        String flowJson = noteData.getString("noteInfo");
        JSONObject jsonObject = JSONObject.fromObject(flowJson);

        String workEffortId = delegator.getNextSeqId("WorkEffort");
        Long revisionNumber = delegator.getNextSeqIdLong("RevisionNumber");
        //创建根节点
        GenericValue rootWorkEffort = delegator.create("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId,
                "workEffortName", jsonObject.getString("nodeName"), "workEffortTypeId", "ROOT_NODE",
                "priority", jsonObject.getLong("nodeId"), "workEffortParentId", templateWorkEffort.getString("workEffortId"),
                "revisionNumber", revisionNumber, "createdByUserLogin",  userLogin.getString("userLoginId")));
        //把审批对象关联到根节点
        ModelEntity modelEntity = genericValue.getModelEntity();
        //重复提交
        delegator.removeByAnd("WorkFlowMember", UtilMisc.toMap("memberEntityName", entityName, "memberEntityId", genericValue.getString(modelEntity.getFirstPkFieldName())));
        delegator.create("WorkFlowMember", UtilMisc.toMap("workFlowMemberId", delegator.getNextSeqId("WorkFlowMember"),
                "workEffortId", workEffortId, "memberEntityName", entityName, "memberEntityId", genericValue.getString(modelEntity.getFirstPkFieldName())));
        rootWorkEffort.set("currentStatusId", "WEPR_COMPLETE");
        rootWorkEffort.store();
    }

}
