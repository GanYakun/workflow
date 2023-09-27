package com.banfftech.services;

import com.banfftech.common.util.CommonUtils;
import com.banfftech.util.ServiceUtils;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.services.OfbizServiceException;
import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.banfftech.util.ServiceUtils.traverseUpdateParentDepartments;
import static org.apache.ofbiz.common.login.LoginServices.getHashType;

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

    /**
     * @param [dctx, context]
     * @Author yyp
     * @Description 主要功能:在某个部门添加成员之后,更新该部门及其所有父级部门的数量
     * @Date 11:24 2023/9/19
     * @EntityTypeName PartyGroup
     * @ServiceName banfftech.updateMemberNumber
     **/
    public static Map<String, Object> updateMemberNumber(DispatchContext dctx, Map<String, Object> context)
            throws GenericServiceException, OfbizServiceException {
        try {
            Delegator delegator = dctx.getDelegator();
            LocalDispatcher dispatcher = dctx.getDispatcher();
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            String partyId = (String) context.get("partyId");
            //使用成员ID查询所在部门(一个成员只能有个一个部门)
            GenericValue department = EntityQuery.use(delegator).from("PartyRelationship")
                    .where(UtilMisc.toMap("partyIdTo", partyId, "roleTypeIdTo", "ORD_EMPLOYEE"))
                    .queryFirst();
            if (UtilValidate.isEmpty(department)) {
                throw new OfbizServiceException("没有找到当前员工所在部门");
            }
            //当前成员所在部门Id
            String currentDepartmentId = department.getString("partyIdFrom");
            //从当前部们开始遍历知道根部门为止(暂时根部们先定死为Company,今后考虑怎么改为动态获取根部们)
            ServiceUtils.traverseUpdateParentDepartments(delegator, dispatcher, currentDepartmentId, userLogin);
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
            GenericValue verifyUserLogin = delegator.findOne("UserLogin",UtilMisc.toMap("userLoginId",userLoginId),false);
            if(UtilValidate.isNotEmpty(verifyUserLogin)){
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
        String roleTypeIdTo = (String) context.get("roleTypeIdTo");
        String partyIdTo = (String) context.get("partyIdTo");
        context.put("fromDate", UtilDateTime.nowTimestamp());

        GenericValue partyRole = delegator.findOne("PartyRole", UtilMisc.toMap("partyId", partyIdTo, "roleTypeId", roleTypeIdTo), false);
        if (UtilValidate.isEmpty(partyRole) && UtilValidate.isNotEmpty(roleTypeIdTo)) {
            Map<String, Object> partyRoleResultMap = dispatcher.runSync("banfftech.createPartyRole", UtilMisc.toMap("userLogin", context.get("userLogin"), "partyId", context.get("partyIdTo"), "roleTypeId", context.get("roleTypeIdTo")));
        }
        CommonUtils.setServiceFieldsAndRun(dctx, context, "banfftech.updatePartyRelationship", (GenericValue) context.get("userLogin"));

        return resultMap;
    }


}
