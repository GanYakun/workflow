package com.banfftech.event;

import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import java.util.Map;

/**
 * @ClassName: OrgStructureAppEvent
 * @Description: TODO
 * @Author: banff
 * @Date: 2023/9/14 14:54
 */
public class OrgStructureAppEvent {
    /**
     * @Author yyp
     * @Description //TODO
     * @Date 14:58 2023/9/14
     * @Edmconfig
     * @ActionName
     **/
    public static Object setEmployeesRole(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws OfbizODataException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) actionParameters.get("member");
        GenericValue member = odataOfbizEntity.getGenericValue();
        String partyId = (String) member.get("partyId");
        try {
            GenericValue managerRole = delegator.findOne("PartyRole",
                    UtilMisc.toMap("partyId", partyId, "roleTypeId", "MANAGER"), false);
            if (UtilValidate.isNotEmpty(managerRole)) {
                dispatcher.runSync("banfftech.deletePartyRole",
                        UtilMisc.toMap("partyId", partyId, "roleTypeId", "MANAGER", "userLogin", userLogin));
            } else {
                dispatcher.runSync("banfftech.createPartyRole",
                        UtilMisc.toMap("partyId", partyId, "roleTypeId", "MANAGER", "userLogin", userLogin));
            }
        } catch (GenericEntityException | GenericServiceException e) {
            throw new OfbizODataException(e.getMessage());
        }
        return member;
    }


    /**
    * @Author yyp
    * @Description 审批通过和不通过
    * @Date 14:15 2023/9/21
    * @Edmconfig approvalManageEdmConfig.xml
    * @ActionName Approved And ApproveRefuse
    **/

    public static void approve(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws OfbizODataException {
        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) actionParameters.get("approval");
        GenericValue approval = odataOfbizEntity.getGenericValue();
        String workEffortPartyAssignmentId = approval.getString("workEffortPartyAssignmentId");
        String comments = (String) actionParameters.get("comments");
        String statusId = (String) actionParameters.get("statusId");

        try {
            dispatcher.runSync("banfftech.updateWorkEffortPartyAssignment",
                    UtilMisc.toMap("workEffortPartyAssignmentId",workEffortPartyAssignmentId,"statusId",statusId,"comments",comments,"userLogin",userLogin));
        } catch (GenericServiceException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }

//    public static Object changeDepartment(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws OfbizODataException {
//        Delegator delegator = (Delegator) oDataContext.get("delegator");
//        LocalDispatcher dispatcher = (LocalDispatcher) oDataContext.get("dispatcher");
//        GenericValue userLogin = (GenericValue) oDataContext.get("userLogin");
//        OdataOfbizEntity odataOfbizEntity = (OdataOfbizEntity) actionParameters.get("member");
//        GenericValue member = odataOfbizEntity.getGenericValue();
//        String partyId = (String) member.get("partyId");
//        try {
//
//        }
//            GenericValue parentDepartment = EntityQuery.use(delegator).from("PartyRelationship")
//                .where(UtilMisc.toMap("partyIdTo", partyId, "roleTypeIdTo", "DEPARTMENT"))
//                .queryFirst();
//        return member;
//    }
}
