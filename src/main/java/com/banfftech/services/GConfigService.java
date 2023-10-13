package com.banfftech.services;

import com.banfftech.common.util.CommonUtils;
import com.banfftech.util.ServiceUtils;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.services.OfbizServiceException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @ClassName: GConfigService
 * @Description: TODO
 * @Author: banff
 * @Date: 2023/9/12 11:03
 */
public class GConfigService {

    public static Map<String, Object> createPartyAndPartyGroup(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            Map<String, Object> partyResult = CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createParty",
                    (String) context.get("userLoginId"));
            context.put("partyId", partyResult.get("partyId"));
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyGroup",
                    (String) context.get("userLoginId"));
            result.put("partyId", partyResult.get("partyId"));
        } catch (GeneralServiceException | GenericEntityException | GenericServiceException e) {
            throw new GenericServiceException(e.getMessage());
        }

        return result;
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
        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {

            Map<String, Object> partyResult = CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createParty",
                    (String) context.get("userLoginId"));
            context.put("partyId", partyResult.get("partyId"));
            context.put("roleTypeId", "DEPARTMENT");
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyRole",
                    (String) context.get("userLoginId"));
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyGroup",
                    (String) context.get("userLoginId"));
            result.put("partyId", partyResult.get("partyId"));
        } catch (GeneralServiceException | GenericEntityException | GenericServiceException e) {
            throw new GenericServiceException(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> createUserGroup(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        try {
            Map<String, Object> partyResult = CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createParty",
                    (String) context.get("userLoginId"));
            context.put("partyId", partyResult.get("partyId"));
            context.put("roleTypeId", "OTHER_ORGANIZATION_U");
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyRole",
                    (String) context.get("userLoginId"));
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createPartyGroup",
                    (String) context.get("userLoginId"));
            result.put("partyId", partyResult.get("partyId"));
        } catch (GeneralServiceException | GenericEntityException | GenericServiceException e) {
            throw new GenericServiceException(e.getMessage());
        }
        return result;
    }

    public static Map<String, Object> updateMemberNumber(DispatchContext dctx, Map<String, Object> context)
            throws GenericServiceException {
        try {
            Delegator delegator = dctx.getDelegator();
            LocalDispatcher dispatcher = dctx.getDispatcher();
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String partyIdFrom = (String) context.get("partyIdFrom");
            //部门添加成员时该字段为空(仅供成员更改部门时使用)
            String oldDepartmentId = (String) context.get("oldPartyIdFrom");

            //第一步:获取新老部门的所有父级部门ID
            Set<String> parentDepartmentIds = ServiceUtils.traverseParentDepartments(delegator, partyIdFrom);
            if (UtilValidate.isNotEmpty(oldDepartmentId)) {
                parentDepartmentIds.addAll(ServiceUtils.traverseParentDepartments(delegator, oldDepartmentId));
            }

            //第二步:更新相关部门的的成员数量
            for (String departmentId : parentDepartmentIds) {
                List<GenericValue> allMembers = new ArrayList<>();
                ServiceUtils.getDepartmentALlMembers(delegator, departmentId, allMembers);
                dispatcher.runSync("banfftech.updatePartyGroup",
                        UtilMisc.toMap("partyId", departmentId, "numEmployees", allMembers.size(), "userLogin", userLogin));
            }

        } catch (GenericEntityException | OfbizODataException e) {
            throw new GenericServiceException(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * @param [dctx, context]
     * @Author yyp
     * @Description 主要作用:在创建成员之后Eca触发创建对应的登录账号和登录权限(默认创建后的初始状态是未激活)
     * @Date 10:33 2023/9/20
     * @EntityTypeName
     * @ServiceName
     **/
    public static Map<String, Object> createUserLoginAndPermission(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException, OfbizServiceException {
        try {
            Delegator delegator = dctx.getDelegator();
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String userLoginId = (String) context.get("phoneMobile");
            GenericValue verifyUserLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", userLoginId), false);
            if (UtilValidate.isNotEmpty(verifyUserLogin)) {
                throw new OfbizServiceException("当前组织内已存在相同的手机号码,请更换后重试");
            }

            context.put("userLoginId", userLoginId);
            context.put("enabled", "Y");
            context.put("currentPassword", CommonUtils.getEncryptedPassword(delegator, "gongsconfig"));
            context.put("groupId", "VISIT");
            context.put("fromDate", UtilDateTime.nowTimestamp());

            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createUserLogin",
                    userLogin.getString("userLoginId"));
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.createUserLoginSecurityGroup",
                    userLogin.getString("userLoginId"));
        } catch (GeneralServiceException | GenericEntityException | GenericServiceException e) {
            throw new GenericServiceException(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }


    public static Map<String, Object> updatePartyRelationshipAndCheckRole(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException, GeneralServiceException, OfbizODataException, GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Map<String, Object> resultMap = ServiceUtil.returnSuccess();
        String partyRelationshipId = (String) context.get("partyRelationshipId");
        String roleTypeIdTo = (String) context.get("roleTypeIdTo");
        String partyIdTo = (String) context.get("partyIdTo");
        context.put("fromDate", UtilDateTime.nowTimestamp());

        GenericValue partyRole = delegator.findOne("PartyRole", UtilMisc.toMap("partyId", partyIdTo, "roleTypeId", roleTypeIdTo), false);
        if (UtilValidate.isEmpty(partyRole) && UtilValidate.isNotEmpty(roleTypeIdTo)) {
            Map<String, Object> partyRoleResultMap = dispatcher.runSync("banfftech.createPartyRole",
                    UtilMisc.toMap("userLogin", context.get("userLogin"), "partyId", context.get("partyIdTo"), "roleTypeId", context.get("roleTypeIdTo")));
        }
        //更新PartyRelationship之前获取partyIdFrom
        GenericValue partyRelationship = delegator.findOne("PartyRelationship", UtilMisc.toMap("partyRelationshipId", partyRelationshipId), false);
        context.put("oldPartyIdFrom", partyRelationship.getString("partyIdFrom"));

        CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.updatePartyRelationship", (GenericValue) context.get("userLogin"));
        //如果是成员和部门之间的关系变更,则更新所有相关部门的成员数量
        if ("ORD_EMPLOYEE".equals(partyRelationship.getString("roleTypeIdTo"))) {
            CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.updateMemberNumber", (GenericValue) context.get("userLogin"));
        }
        return resultMap;
    }


}
