package com.banfftech.event;

import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 用于管理可审批的业务对象
 *
 * @author scy
 * @date 2023/9/11
 */
public class ApprovalObjectEvent {
    private static final List<String> TYPE_TIME = UtilMisc.toList("date-time", "date", "time");
    private static final List<String> TYPE_BOOL = UtilMisc.toList("indicator");
    private static final List<String> TYPE_NUM = UtilMisc.toList("currency-amount", "currency-precise", "fixed-point", "floating-point", "numeric");

    /**
     * 初始化字段类型
     */
    public static void initFieldType(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.get("processEntity");
        String processEntityId = (String) ofbizEntity.getPropertyValue("processEntityId");
        String processEntityName = (String) ofbizEntity.getPropertyValue("processEntityName");
        ModelEntity modelEntity = delegator.getModelEntity(processEntityName);
        List<String> automaticFieldNames = modelEntity.getAutomaticFieldNames();
        Iterator<ModelField> fieldsIterator = modelEntity.getFieldsIterator();

        List<GenericValue> processFields = EntityQuery.use(delegator).from("ProcessField").where("processEntityId", processEntityId).queryList();
        for (GenericValue processField : processFields) {
            delegator.removeByAnd("ProcessFieldLabel", UtilMisc.toMap("processFieldId", processField.getString("processFieldId")));
        }
        delegator.removeByAnd("ProcessField", UtilMisc.toMap("processEntityId", processEntityId));
        while (fieldsIterator.hasNext()) {
            ModelField field = fieldsIterator.next();
            if (automaticFieldNames.contains(field.getName())) {
                continue;
            }
            String fieldType = getFieldType(field);
            delegator.create("ProcessField", UtilMisc.toMap("processEntityId", processEntityId, "processFieldId", delegator.getNextSeqId("ProcessField"),
                    "processFieldName", field.getName(), "processFieldTypeId", fieldType));
        }
    }

    /**
     * 设置描述
     */
    public static void updateData(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.get("processField");
        String processFieldTypeId = (String) actionParameters.get("processFieldTypeId");
        Boolean asCondition = (Boolean) actionParameters.get("asCondition");
        String valueTypeId = (String) actionParameters.get("valueTypeId");
        if (UtilValidate.isEmpty(processFieldTypeId)) {
            processFieldTypeId = null;
        }
        String asConditionStr = asCondition ? "Y" : "N";
        String description = (String) actionParameters.get("description");
        delegator.storeByCondition("ProcessField", UtilMisc.toMap("description", description, "valueTypeId", valueTypeId, "processFieldTypeId", processFieldTypeId,
                "asCondition", asConditionStr), EntityCondition.makeCondition(ofbizEntity.getGenericValue().getPrimaryKey()));
    }


    /**
     * 初始化字段类型
     */
    public static void addLabel(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        Locale locale = (Locale) oDataContext.get("locale");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.get("processField");
        String language = (String) actionParameters.get("language");
        if (UtilValidate.isEmpty(language)) {
            language = locale.getLanguage();
        }
        String value = (String) actionParameters.get("value");
        String processFieldId = (String) ofbizEntity.getPropertyValue("processFieldId");
        GenericValue label = EntityQuery.use(delegator).from("ProcessFieldLabel").where("processFieldId", processFieldId, "language", language).queryFirst();
        if (UtilValidate.isNotEmpty(label)) {
            label.set("value", value);
            label.store();
        } else {
            delegator.create("ProcessFieldLabel", UtilMisc.toMap("processFieldLabelId", delegator.getNextSeqId("ProcessFieldLabel"),
                    "processFieldId", processFieldId, "language", language, "value", value));
        }
    }

    /**
     * 获取文件类型枚举值
     */
    private static String getFieldType(ModelField modelField) {
        String fieldType = modelField.getType();
        String ofbizFieldName = modelField.getName();
        if (ofbizFieldName.toUpperCase().endsWith("STATUSID")) {
            return "STATUS";
        }
        if (ofbizFieldName.toUpperCase().endsWith("TYPEID")) {
            return "TYPE";
        }
        if (ofbizFieldName.toUpperCase().endsWith("ENUMID")) {
            return "ENUM";
        }
        if (TYPE_TIME.contains(fieldType)) {
            return "DATE_TIME";
        }
        if (TYPE_BOOL.contains(fieldType)) {
            return "BOOL";
        }
        if (TYPE_NUM.contains(fieldType)) {
            return "NUMBER";
        }
        //default
        return "STRING";

    }


}
