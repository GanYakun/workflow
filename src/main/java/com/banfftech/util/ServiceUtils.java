package com.banfftech.util;

import com.dpbird.odata.OfbizODataException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author scy
 * @date: 2021/9/23
 */
public class ServiceUtils {
    public static BigDecimal staticBigDecimal = BigDecimal.ONE;

    public static Set<String> traverseParentDepartments(Delegator delegator, LocalDispatcher dispatcher, String currentDepartmentId)
            throws GenericEntityException {
        Set<String> allParentDepartmentIds = new HashSet<>();
        //获取该部门和父级部门的Relation
        GenericValue parentDepartment = EntityQuery.use(delegator).from("PartyRelationship")
                .where(UtilMisc.toMap("partyIdTo", currentDepartmentId, "roleTypeIdTo", "DEPARTMENT"))
                .queryFirst();
        //如果没有父级部门(则当前部门就是根部门,更新当前部门的人数)
        if (UtilValidate.isNotEmpty(parentDepartment)) {
            allParentDepartmentIds.add(parentDepartment.getString("partyIdFrom"));
            Set<String> prentDepartmentIds = traverseParentDepartments(delegator, dispatcher, parentDepartment.getString("partyIdFrom"));
            allParentDepartmentIds.addAll(prentDepartmentIds);
        }

        return allParentDepartmentIds;
    }

    /**
     * @param [delegator, departmentId, allMembers]
     * @Author yyp
     * @Description //使用部门Id,查询并返回该部门及其子部门的所有成员
     * @Date 11:16 2023/10/12
     **/
    public static void getDepartmentALlMembers(Delegator delegator, String departmentId, List<GenericValue> allMembers)
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
        } catch (GenericEntityException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }

}
