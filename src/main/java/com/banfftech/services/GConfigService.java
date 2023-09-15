package com.banfftech.services;

import com.banfftech.common.util.CommonUtils;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.*;

import java.util.List;
import java.util.Map;

/**
 * @ClassName: GConfigService
 * @Description: TODO
 * @Author: banff
 * @Date: 2023/9/12 11:03
 */
public class GConfigService {

    public static Map<String, Object> createPartyAndPartyGroup(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        try {
            Map<String, Object> partyResult = CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createParty",
                    (String) context.get("userLoginId"));
            context.put("partyId", partyResult.get("partyId"));
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyGroup",
                    (String) context.get("userLoginId"));
        } catch (GeneralServiceException | GenericEntityException | GenericServiceException e) {
            throw new GenericServiceException(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updatePartyAndPartyGroup(DispatchContext dctx, Map<String, Object> context)
            throws GenericServiceException {
        try {
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.updateParty",
                    (String) context.get("userLoginId"));
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.updatePartyGroup",
                    (String) context.get("userLoginId"));
        } catch (GeneralServiceException | GenericEntityException | GenericServiceException e) {
            throw new GenericServiceException(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> deletePartyAndPartyGroup(DispatchContext dctx, Map<String, Object> context)
            throws GenericServiceException {
        try {
            String partyId = (String) context.get("partyId");

            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.deletePartyRelationShip",
                    (String) context.get("userLoginId"));
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.deletePartyGroup",
                    (String) context.get("userLoginId"));
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.deleteParty",
                    (String) context.get("userLoginId"));
        } catch (GeneralServiceException | GenericEntityException | GenericServiceException e) {
            throw new GenericServiceException(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> createDepartment(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        try {
            Map<String, Object> partyResult = CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createParty",
                    (String) context.get("userLoginId"));
            context.put("partyId", partyResult.get("partyId"));
            context.put("roleTypeId", "DEPARTMENT");
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyRole",
                    (String) context.get("userLoginId"));
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyGroup",
                    (String) context.get("userLoginId"));
        } catch (GeneralServiceException | GenericEntityException | GenericServiceException e) {
            throw new GenericServiceException(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateMemberNumber(DispatchContext dctx, Map<String, Object> context)
            throws GenericServiceException {
        try {
            Delegator delegator = dctx.getDelegator();
            LocalDispatcher dispatcher = dctx.getDispatcher();
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String partyId = (String) context.get("partyId");
            GenericValue department = EntityQuery.use(delegator).from("PartyRelationship")
                    .where(UtilMisc.toMap("partyIdTo", partyId, "roleTypeIdTo", "EMPLOYEE", "roleTypeIdFrom", "DEPARTMENT"))
                    .queryFirst();
            List<GenericValue> members = EntityQuery.use(delegator).from("PartyRelationship")
                    .where(UtilMisc.toMap("partyIdFrom", department.getString("partyIdFrom"), "roleTypeIdTo", "EMPLOYEE", "roleTypeIdFrom", "DEPARTMENT"))
                    .queryList();
            dispatcher.runSync("banfftech.updatePartyGroup",UtilMisc.toMap("partyId",department.getString("partyIdFrom"),"numEmployees",members.size(),"userLogin",userLogin));
        } catch (GenericEntityException e) {
            throw new GenericServiceException(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }
}
