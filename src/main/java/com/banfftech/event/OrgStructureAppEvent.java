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
