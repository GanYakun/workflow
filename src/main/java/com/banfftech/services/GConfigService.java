package com.banfftech.services;

import com.banfftech.common.util.CommonUtils;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GeneralServiceException;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.ServiceUtil;

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
            context.put("partyIdFrom", "Company");
            context.put("partyIdTo", partyResult.get("partyId"));
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyRelationship",
                    (String) context.get("userLoginId"));
        } catch (GeneralServiceException | GenericEntityException | GenericServiceException e) {
            throw new GenericServiceException(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
}
