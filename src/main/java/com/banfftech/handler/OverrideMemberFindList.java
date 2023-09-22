package com.banfftech.handler;

import com.dpbird.odata.OfbizODataException;
import com.dpbird.odata.edm.OdataOfbizEntity;
import com.dpbird.odata.handler.DefaultEntityHandler;
import com.dpbird.odata.handler.HandlerResults;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.server.api.uri.queryoption.QueryOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: OverrideEnumerationFindList
 * @Description: TODO
 * @Author: banff
 * @Date: 2023/8/10 12:16
 */
public class OverrideMemberFindList extends DefaultEntityHandler {

    @Override
    public HandlerResults findList(Map<String, Object> odataContext, EdmBindingTarget edmBindingTarget, Map<String, Object> primaryKey,
                                   Map<String, QueryOption> queryOptions, Map<String, Object> navigationParam) throws OfbizODataException {
        Delegator delegator = (Delegator) odataContext.get("delegator");

        //如果不是一段式并且NavigationName=AllMember
        if (UtilValidate.isNotEmpty(navigationParam)) {
            EdmNavigationProperty edmNavigationProperty = (EdmNavigationProperty) navigationParam.get("edmNavigationProperty");
            String navigationPropertyName = edmNavigationProperty.getName();
            if ("AllMember".equals(navigationPropertyName)) {
                OdataOfbizEntity entity = (OdataOfbizEntity) navigationParam.get("entity");
                GenericValue genericValue = entity.getGenericValue();
                String departmentId = genericValue.getString("partyId");

                List<GenericValue> allMembers = new ArrayList<>();
                getDepartmentALlMembers(delegator, departmentId, allMembers);
                return new HandlerResults(allMembers.size(), allMembers);
            }
        }
        //否则(如果是一段式或者NavigationName!=AllMember)直接调用Super
        return super.findList(odataContext, edmBindingTarget, primaryKey, queryOptions, navigationParam);


    }

    private void getDepartmentALlMembers(Delegator delegator, String departmentId, List<GenericValue> allMembers)
            throws OfbizODataException {
        //获取该部门的所有子部门
        try {
            List<GenericValue> subDepartments = EntityQuery.use(delegator).from("PartyRelationship")
                    .where(UtilMisc.toMap("partyIdFrom", departmentId, "roleTypeIdTo", "DEPARTMENT"))
                    .queryList();

            //获取当前部门的所有成员列表
            List<GenericValue> membersId = EntityQuery.use(delegator).from("PartyRelationship")
                    .where(UtilMisc.toMap("partyIdFrom", departmentId, "roleTypeIdTo", "ORD_EMPLOYEE"))
                    .getFieldList("partyIdTo");

            EntityCondition membersIdCondition = EntityCondition.makeCondition("partyId", EntityOperator.IN, membersId);
            List<GenericValue> members = EntityQuery.use(delegator).from("PartyAndContact").where(membersIdCondition).queryList();
            allMembers.addAll(members);

            if (UtilValidate.isNotEmpty(subDepartments)) {
                for (GenericValue subDepartment : subDepartments) {
                    getDepartmentALlMembers(delegator, subDepartment.getString("partyIdTo"), allMembers);
                }
            }
            return;
        } catch (GenericEntityException e) {
            throw new OfbizODataException(e.getMessage());
        }
    }
}
