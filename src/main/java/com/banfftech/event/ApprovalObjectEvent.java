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
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.get("dbEntity");
        String dbEntityId = (String) ofbizEntity.getPropertyValue("dbEntityId");
        String dbEntityName = (String) ofbizEntity.getPropertyValue("dbEntityName");
        ModelEntity modelEntity = delegator.getModelEntity(dbEntityName);
        List<String> automaticFieldNames = modelEntity.getAutomaticFieldNames();
        Iterator<ModelField> fieldsIterator = modelEntity.getFieldsIterator();

        List<GenericValue> dbFields = EntityQuery.use(delegator).from("DBField").where("dbEntityId", dbEntityId).queryList();
        for (GenericValue dbField : dbFields) {
            delegator.removeByAnd("DBFieldLabel", UtilMisc.toMap("dbFieldId", dbField.getString("dbFieldId")));
        }
        delegator.removeByAnd("DBField", UtilMisc.toMap("dbEntityId", dbEntityId));
        while (fieldsIterator.hasNext()) {
            ModelField field = fieldsIterator.next();
            if (automaticFieldNames.contains(field.getName())) {
                continue;
            }
            String fieldType = getFieldType(field);
            delegator.create("DBField", UtilMisc.toMap("dbEntityId", dbEntityId, "dbFieldId", delegator.getNextSeqId("DBField"),
                    "dbFieldName", field.getName(), "dbFieldTypeId", fieldType));
        }
    }

    /**
     * 初始化字段类型
     */
    public static void addLabel(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        Locale locale = (Locale) oDataContext.get("locale");
        OdataOfbizEntity ofbizEntity = (OdataOfbizEntity) actionParameters.get("dbField");
        String language = (String) actionParameters.get("language");
        if (UtilValidate.isEmpty(language)) {
            language = locale.getLanguage();
        }
        String value = (String) actionParameters.get("value");
        String dbFieldId = (String) ofbizEntity.getPropertyValue("dbFieldId");
        GenericValue label = EntityQuery.use(delegator).from("DBFieldLabel").where("dbFieldId", dbFieldId, "language", language).queryFirst();
        if (UtilValidate.isNotEmpty(label)) {
            label.set("value", value);
            label.store();
        } else {
            delegator.create("DBFieldLabel", UtilMisc.toMap("dbFieldLabelId", delegator.getNextSeqId("DBFieldLabel"),
                    "dbFieldId", dbFieldId, "language", language, "value", value));
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
