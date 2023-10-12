package com.banfftech.util;

import com.dpbird.odata.OdataParts;
import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.service.*;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Property;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author scy
 * @date: 2021/9/23
 */
public class ServiceUtils {
    public static BigDecimal staticBigDecimal = BigDecimal.ONE;

    /**
     * @param [delegator, dispatcher, currentDepartmentId, userLogin]
     * @Author yyp
     * @Description 主要功能:遍历所有父级部门,并更新其部门人员数量(此方法考虑,使用方法计算而不是直接加1)
     * @Date 14:15 2023/9/19
     **/
    public static void traverseUpdateParentDepartments(Delegator delegator, LocalDispatcher dispatcher, String currentDepartmentId, GenericValue userLogin)
            throws OfbizODataException, GenericServiceException, GenericEntityException {

        //获取该部门和父级部门的Relation
        GenericValue parentDepartment = EntityQuery.use(delegator).from("PartyRelationship")
                .where(UtilMisc.toMap("partyIdTo", currentDepartmentId, "roleTypeIdTo", "DEPARTMENT"))
                .queryFirst();
        //如果没有父级部门(则当前部门就是根部门,更新当前部门的人数)
        if (UtilValidate.isEmpty(parentDepartment)) {
            updateDepartmentMemberNumber(delegator, dispatcher, currentDepartmentId, staticBigDecimal, userLogin);
            return;
        }
        //如果有父级部门,更新当前部门人数,并继续遍历更新父级部门
        updateDepartmentMemberNumber(delegator, dispatcher, currentDepartmentId, staticBigDecimal, userLogin);
        traverseUpdateParentDepartments(delegator, dispatcher, parentDepartment.getString("partyIdFrom"), userLogin);
    }

    /**
     * @param [delegator, dispatcher, departmentId, userLogin]
     * @Author yyp
     * @Description 主要功能:根据部门ID,更新部门人数使其+1
     * @Date 14:18 2023/9/19
     **/

    public static void updateDepartmentMemberNumber(Delegator delegator, LocalDispatcher dispatcher, String departmentId, BigDecimal staticBigDecimal, GenericValue userLogin)
            throws GenericEntityException, GenericServiceException {

        //查询当前部门
        GenericValue Department = delegator.findOne("PartyGroup",
                UtilMisc.toMap("partyId", departmentId), false);
        //获取当前部门人数,并加一
        Long numEmployees = Department.getLong("numEmployees");
        numEmployees += 1;
        dispatcher.runSync("banfftech.updatePartyGroup",
                UtilMisc.toMap("partyId", departmentId, "numEmployees", numEmployees, "userLogin", userLogin));
    }

//    public static void traverseUpdateParentDepartments(Delegator delegator, LocalDispatcher dispatcher, String currentDepartmentId, GenericValue userLogin)
//            throws OfbizODataException, GenericServiceException, GenericEntityException {
//
//        //获取该部门和父级部门的Relation
//        GenericValue parentDepartment = EntityQuery.use(delegator).from("PartyRelationship")
//                .where(UtilMisc.toMap("partyIdTo", currentDepartmentId, "roleTypeIdTo", "DEPARTMENT"))
//                .queryFirst();
//        //如果没有父级部门(则当前部门就是根部门,更新当前部门的人数)
//        if (UtilValidate.isEmpty(parentDepartment)) {
//            updateDepartmentMemberNumber(delegator, dispatcher, currentDepartmentId, staticBigDecimal,userLogin);
//            return;
//        }
//        //如果有父级部门,更新当前部门人数,并继续遍历更新父级部门
//        updateDepartmentMemberNumber(delegator, dispatcher, currentDepartmentId, staticBigDecimal,userLogin);
//
//
//        traverseUpdateParentDepartments(delegator, dispatcher, parentDepartment.getString("partyIdFrom"), userLogin);
//    }
//
//    /**
//     * @param [delegator, dispatcher, departmentId, userLogin]
//     * @Author yyp
//     * @Description 主要功能:根据部门ID,更新部门人数使其+1
//     * @Date 14:18 2023/9/19
//     **/
//
//    public static void updateDepartmentMemberNumber(Delegator delegator, LocalDispatcher dispatcher, String departmentId, BigDecimal staticBigDecimal,GenericValue userLogin)
//            throws GenericEntityException, GenericServiceException {
//
//        //查询当前部门
//        GenericValue Department = delegator.findOne("PartyGroup",
//                UtilMisc.toMap("partyId", departmentId), false);
//        //获取当前部门人数,并加一
////        BigDecimal numEmployees = Department.getBigDecimal("numEmployees");
////        numEmployees = numEmployees.add(BigDecimal.ONE);
////        dispatcher.runSync("banfftech.updatePartyGroup",
////                UtilMisc.toMap("partyId", departmentId, "numEmployees", numEmployees, "userLogin", userLogin));
//        List<GenericValue> membersId = EntityQuery.use(delegator).from("PartyRelationship")
//                .where(UtilMisc.toMap("partyIdFrom", departmentId, "roleTypeIdTo", "ORD_EMPLOYEE"))
//                .getFieldList("partyIdTo");
//        EntityCondition membersIdCondition = EntityCondition.makeCondition("partyId", EntityOperator.IN, membersId);
//        List<GenericValue> members = EntityQuery.use(delegator).from("PartyAndContact").where(membersIdCondition).queryList();
//        staticBigDecimal = staticBigDecimal.add(BigDecimal.valueOf(members.size()));
//        dispatcher.runSync("banfftech.updatePartyGroup",
//                UtilMisc.toMap("partyId", departmentId, "numEmployees", members.size(), "userLogin", userLogin));
//    }
//
//
//    private void getDepartmentALlMembers(Delegator delegator, String departmentId, List<GenericValue> allMembers)
//            throws OfbizODataException {
//        //获取该部门的所有子部门
//        try {
//            List<GenericValue> subDepartments = EntityQuery.use(delegator).from("PartyRelationship")
//                    .where(UtilMisc.toMap("partyIdFrom", departmentId, "roleTypeIdTo", "DEPARTMENT"))
//                    .queryList();
//
//            //获取当前部门的所有成员列表
//            List<GenericValue> membersId = EntityQuery.use(delegator).from("PartyRelationship")
//                    .where(UtilMisc.toMap("partyIdFrom", departmentId, "roleTypeIdTo", "ORD_EMPLOYEE"))
//                    .getFieldList("partyIdTo");
//
//            EntityCondition membersIdCondition = EntityCondition.makeCondition("partyId", EntityOperator.IN, membersId);
//            List<GenericValue> members = EntityQuery.use(delegator).from("PartyAndContact").where(membersIdCondition).queryList();
//            allMembers.addAll(members);
//
//            if (UtilValidate.isNotEmpty(subDepartments)) {
//                for (GenericValue subDepartment : subDepartments) {
//                    getDepartmentALlMembers(delegator, subDepartment.getString("partyIdTo"), allMembers);
//                }
//            }
//            return;
//        } catch (GenericEntityException e) {
//            throw new OfbizODataException(e.getMessage());
//        }
//    }


    /**
     * @param [delegator, departmentId, allMembers]
     * @Author yyp
     * @Description //使用部门Id,查询并返回该部门及其子部门的所有成员
     * @Date 11:16 2023/10/12
     **/
    public static List<GenericValue> getDepartmentALlMembers(Delegator delegator, String departmentId, List<GenericValue> allMembers)
            throws OfbizODataException {
        try {
            //获取该部门的所有子部门
            List<GenericValue> subDepartments = EntityQuery.use(delegator).from("PartyRelationship")
                    .where(UtilMisc.toMap("partyIdFrom", departmentId, "roleTypeIdTo", "DEPARTMENT"))
                    .queryList();

            //获取当前部门的所有直属成员partyId
            List<String> membersId = EntityQuery.use(delegator).from("PartyRelationship")
                    .where(UtilMisc.toMap("partyIdFrom", departmentId, "roleTypeIdTo", "ORD_EMPLOYEE"))
                    .getFieldList("partyIdTo");

            //获取并添加当前部门所有直属成员
            EntityCondition membersIdCondition = EntityCondition.makeCondition("partyId", EntityOperator.IN, membersId);
            List<GenericValue> members = EntityQuery.use(delegator).from("PartyAndContact").where(membersIdCondition).queryList();
            allMembers.addAll(members);

            //遍历当前部门的所有子部门,进行以上递归循环
            if (UtilValidate.isNotEmpty(subDepartments)) {
                for (GenericValue subDepartment : subDepartments) {
                    getDepartmentALlMembers(delegator, subDepartment.getString("partyIdTo"), allMembers);
                }
            }
            return allMembers;
        } catch (GenericEntityException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }

}
