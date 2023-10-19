package com.banfftech.event;

import com.dpbird.odata.OfbizODataException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
        JSONObject jsonObject = JSONObject.fromObject(data);

        JSONObject entityType = jsonObject.getJSONObject("obj");
        String description = entityType.getString("Description");
        String name = entityType.getString("Name");
        String ofbizEntity = entityType.getString("OfbizEntity");
        String dbEntityId = entityType.getString("dbEntityId");
        String entitySetName = (String) entityType.get("EntitySetName");
        if (UtilValidate.isEmpty(entitySetName)) {
            entitySetName = ofbizEntity;
        }
        boolean autoProperties = entityType.getBoolean("AutoProperties");
        String entityCondition = (String) entityType.get("EntityCondition");

        //创建EdmEntityType
        String edmEntityTypeId = delegator.getNextSeqId("EdmEntityType");
        delegator.create("EdmEntityType", UtilMisc.toMap("edmEntityTypeId", edmEntityTypeId, "name", name,
                "autoProperties", autoProperties ? "Y" : "N", "entitySetName", entitySetName, "entityCondition", entityCondition,
                "dbEntityId", dbEntityId, "edmServiceId", edmServiceId, "description", description));
        //创建EdmProperty
        JSONArray propertyArr = entityType.getJSONArray("Property");
        for (int i = 0; i < propertyArr.size(); i++) {
            JSONObject property = propertyArr.getJSONObject(i);
            String propertyName = property.getString("Name");
            String hidden = property.getString("Hidden");
            String computed = property.getString("Computed");
            String fieldControl = property.getString("FieldControl");
            String immutable = property.getString("Immutable");
            String label = property.getString("Label");
            String edmPropertyId = delegator.getNextSeqId("EdmProperty");

            delegator.create("EdmProperty", UtilMisc.toMap("edmEntityTypeId", edmEntityTypeId, "edmPropertyId", edmPropertyId,
                    "name", propertyName, "hidden", hidden, "computed", computed, "fieldControl", fieldControl, "immutable", immutable, "label", label));
        }
        loadEdmService(delegator, edmServiceId);
//        JSONArray navigationArr = entityType.getJSONArray("NavigationProperty");
//        for (int i = 0; i < navigationArr.size(); i++) {
//            JSONObject property = navigationArr.getJSONObject(i);
//            String navigationName = property.getString("Name");
//            String type = property.getString("Type");
//            String auto = property.getString("Auto");
//            String relationName = property.getString("RelationName");
//            String relations = property.getString("Relations");
//            String edmNavigationPropertyId = delegator.getNextSeqId("EdmNavigationProperty");
//            delegator.create("EdmNavigationProperty", UtilMisc.toMap("edmEntityTypeId", edmEntityTypeId, "edmNavigationPropertyId", edmNavigationPropertyId,
//                    "name", navigationName, "type", type, "relations",  relations));
//        }
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
                ofbizEdm.appendChild(entityTypeEle);
                //TODO: Navigation...
            }
            String edmXmlContent = UtilXml.writeXmlDocument(document);
            delegator.storeByCondition("EdmService", UtilMisc.toMap("edmContent", edmXmlContent),
                    EntityCondition.makeCondition("edmServiceId", edmServiceId));
        } catch (Exception e) {
            throw new OfbizODataException(e.getMessage());
        }
    }

}
