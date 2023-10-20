package com.banfftech.event;

import com.dpbird.odata.OfbizODataException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelRelation;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author scy
 * @date 2023/10/18
 */
public class EdmServiceEvent {

    /**
     * EdmConfig 根元素模板
     */
    private static final String EDM_ROOT = "<ofbiz-edm xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"https://dev.odatalab.com/dtds/edmconfig.xsd\"></ofbiz-edm>";


    /**
     * 初始化字段类型
     */
    public static void createEntityType(Map<String, Object> oDataContext, Map<String, Object> actionParameters, EdmBindingTarget edmBindingTarget) throws GenericEntityException, OfbizODataException {
        Delegator delegator = (Delegator) oDataContext.get("delegator");
        Locale locale = (Locale) oDataContext.get("locale");
        String edmServiceId = (String) actionParameters.get("edmServiceId");
        String data = (String) actionParameters.get("data");
        saveEdmContent(delegator, edmServiceId, "json", data);
        JSONObject entityType = JSONObject.fromObject(data);
        String description = (String) entityType.get("description");
        String name = (String) entityType.get("name");
        String ofbizEntity = (String) entityType.get("ofbizEntity");
        String edmEntityTypeId = (String) entityType.get("edmEntityTypeId");
        String dbEntityId = (String) entityType.get("dbEntityId");
        String entitySetName = (String) entityType.get("entitySetName");
        if (UtilValidate.isEmpty(entitySetName)) {
            entitySetName = name;
        }
        boolean autoProperties = entityType.getBoolean("autoProperties");
        String entityCondition = (String) entityType.get("entityCondition");
        GenericValue edmEntityType = EntityQuery.use(delegator).from("EdmEntityType").where("edmServiceId", edmServiceId, "name", name).queryFirst();
        if (UtilValidate.isNotEmpty(edmEntityType)) {
            throw new OfbizODataException("Duplicate definition: " + name);
        }
        //创建EdmEntityType
        Timestamp createdDate = UtilDateTime.nowTimestamp();
        if (UtilValidate.isNotEmpty(edmEntityTypeId)) {
            //先删除再创建
            Map<String, Object> primaryKey = UtilMisc.toMap("edmEntityTypeId", edmEntityTypeId);
            edmEntityType = delegator.findOne("EdmEntityType", primaryKey, false);
            createdDate = edmEntityType.getTimestamp("createdDate");
            delegator.removeByAnd("EdmProperty", primaryKey);
            delegator.removeByAnd("EdmNavigationProperty", primaryKey);
            edmEntityType.remove();
        } else {
            edmEntityTypeId = delegator.getNextSeqId("EdmEntityType");
        }
        delegator.create("EdmEntityType", UtilMisc.toMap("edmEntityTypeId", edmEntityTypeId, "name", name,
                "autoProperties", autoProperties ? "Y" : "N", "entitySetName", entitySetName, "entityCondition", entityCondition,
                "dbEntityId", dbEntityId, "edmServiceId", edmServiceId, "description", description, "sourceJson", data, "createdDate", createdDate));
        //创建EdmProperty
        JSONArray propertyArr = entityType.getJSONArray("property");
        for (int i = 0; i < propertyArr.size(); i++) {
            JSONObject property = propertyArr.getJSONObject(i);
            String propertyName = (String) property.get("name");
            String hidden = (String) property.get("hidden");
            String computed = (String) property.get("computed");
            String fieldControl = (String) property.get("fieldControl");
            String immutable = (String) property.get("immutable");
            String label = (String) property.get("label");
            String edmPropertyId = delegator.getNextSeqId("EdmProperty");
            delegator.create("EdmProperty", UtilMisc.toMap("edmEntityTypeId", edmEntityTypeId, "edmPropertyId", edmPropertyId,
                    "name", propertyName, "hidden", hidden, "computed", computed, "fieldControl", fieldControl, "immutable", immutable, "label", label));
        }

        JSONArray navigationArr = entityType.getJSONArray("navigationProperty");
        for (int i = 0; i < navigationArr.size(); i++) {
            JSONObject property = navigationArr.getJSONObject(i);
            String navigationName = (String) property.get("name");
            String type = (String) property.get("type");
            String auto = (String) property.get("auto");
            String relationName = (String) property.get("relationName");
            if (UtilValidate.isEmpty(navigationName)) {
                navigationName = relationName;
            }
            String isCollection;
            if (UtilValidate.isNotEmpty(property.get("isCollection"))) {
                isCollection = property.get("isCollection").toString();
            } else {
                ModelEntity modelEntity = delegator.getModelEntity(ofbizEntity);
                ModelRelation relation = modelEntity.getRelation(relationName);
                isCollection = relation.getType().contains("one") ? "N" : "Y";
            }
            String relations = (String) property.get("relations");
            String edmNavigationPropertyId = delegator.getNextSeqId("EdmNavigationProperty");
            delegator.create("EdmNavigationProperty", UtilMisc.toMap("edmEntityTypeId", edmEntityTypeId,
                    "edmNavigationPropertyId", edmNavigationPropertyId, "name", navigationName, "type", type,
                    "relations", relations, "isCollection", isCollection));
        }
        loadEdmService(delegator, edmServiceId);
    }


    private static void loadEdmService(Delegator delegator, String edmServiceId) throws OfbizODataException {
        try {
            Document document = UtilXml.readXmlDocument(EDM_ROOT);
            Node ofbizEdm = document.getFirstChild();
            List<GenericValue> entityTypes = delegator.findByAnd("EdmEntityType", UtilMisc.toMap("edmServiceId", edmServiceId), null, false);
            for (GenericValue entityType : entityTypes) {
                Element entityTypeEle = document.createElement("EntityType");
                entityTypeEle.setAttribute("Name", entityType.getString("name"));
                entityTypeEle.setAttribute("AutoProperties", entityType.getBoolean("autoProperties").toString());
                entityTypeEle.setAttribute("EntitySetName", entityType.getString("entitySetName"));
                entityTypeEle.setAttribute("EntityCondition", entityType.getString("entityCondition"));
                GenericValue dbEntity = entityType.getRelatedOne("DBEntity", false);
                entityTypeEle.setAttribute("OfbizEntity", dbEntity.getString("dbEntityName"));
                List<GenericValue> edmProperties = entityType.getRelated("EdmProperty", null, null, false);
                for (GenericValue edmProperty : edmProperties) {
                    Element propertyEle = UtilXml.addChildElement(entityTypeEle, "Property", document);
                    propertyEle.setAttribute("Name", edmProperty.getString("name"));
                    propertyEle.setAttribute("Hidden", edmProperty.getString("hidden"));
                    propertyEle.setAttribute("Computed", edmProperty.getString("computed"));
                    propertyEle.setAttribute("FieldControl", edmProperty.getString("fieldControl"));
                    propertyEle.setAttribute("Immutable", edmProperty.getString("immutable"));
                    propertyEle.setAttribute("Label", edmProperty.getString("label"));
                }
                List<GenericValue> edmNavigationProperties = entityType.getRelated("EdmNavigationProperty", null, null, false);
                for (GenericValue edmNavigation : edmNavigationProperties) {
                    Element navigationEle = UtilXml.addChildElement(entityTypeEle, "NavigationProperty", document);
                    navigationEle.setAttribute("Name", edmNavigation.getString("name"));
                    navigationEle.setAttribute("Type", edmNavigation.getString("type"));
                    navigationEle.setAttribute("Relations", edmNavigation.getString("relations"));
                    navigationEle.setAttribute("IsCollection", edmNavigation.getBoolean("isCollection").toString());
                }
                ofbizEdm.appendChild(entityTypeEle);
            }
            String edmXmlContent = UtilXml.writeXmlDocument(document);
            saveEdmContent(delegator, edmServiceId, "xml", edmXmlContent);
        } catch (Exception e) {
            throw new OfbizODataException(e.getMessage());
        }
    }

    private static void saveEdmContent(Delegator delegator, String edmServiceId, String format, String edmContent) throws GenericEntityException {
        GenericValue edmServiceContent = EntityQuery.use(delegator).from("EdmServiceContent").where("edmServiceId", edmServiceId, "format", format).queryFirst();
        if (UtilValidate.isEmpty(edmServiceContent)) {
            //create
            String edmServiceContentId = delegator.getNextSeqId("EdmServiceContent");
            delegator.create("EdmServiceContent", UtilMisc.toMap("edmServiceContentId", edmServiceContentId,
                    "edmServiceId", edmServiceId, "format", format, "edmContent", edmContent));
        } else {
            //update
            edmServiceContent.set("edmContent", edmContent);
            edmServiceContent.store();
        }
    }

}
