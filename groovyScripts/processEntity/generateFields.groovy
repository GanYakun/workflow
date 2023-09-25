import com.dpbird.odata.edm.OdataOfbizEntity
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery

module = "generateFields.groovy"

def generateFields(Map<String, Object> context) {
    List<OdataOfbizEntity> entityList = context.parameters.entityList;
    entityList.each { entity ->
        GenericValue processEntity = (GenericValue) entity.getGenericValue();
        String processEntityId = processEntity.getString("processEntityId");

        GenericValue mainProcess = EntityQuery.use(delegator).from("MainProcess")
                .where("processEntityId", processEntityId, "statusId", "PROCESS_ENABLED").queryFirst();
        List<GenericValue> processFields = processEntity.getRelated("ProcessField", UtilMisc.toMap("isOpen", "Y"), null, false);
        Boolean hasActiveFlow = UtilValidate.isNotEmpty(mainProcess);
        BigDecimal configFieldTotal = new BigDecimal(processFields.size());
        entity.addProperty("hasActiveFlow", hasActiveFlow);
        entity.addProperty("configFieldTotal", configFieldTotal);
    }

    return entityList;
}
