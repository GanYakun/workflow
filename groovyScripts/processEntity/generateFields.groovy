import com.dpbird.odata.edm.OdataOfbizEntity
import org.apache.ofbiz.base.util.UtilMisc
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery

module = "generateFields.groovy"

def generateFields(Map<String, Object> context) {
    List<OdataOfbizEntity> entityList = context.parameters.entityList;
    entityList.each { entity ->
        GenericValue dbEntity = (GenericValue) entity.getGenericValue();
        String dbEntityId = dbEntity.getString("dbEntityId");

        GenericValue mainProcess = EntityQuery.use(delegator).from("MainProcess")
                .where("dbEntityId", dbEntityId, "statusId", "PROCESS_ENABLED").queryFirst();
        List<GenericValue> dbFields = dbEntity.getRelated("DBField", UtilMisc.toMap("isOpen", "Y"), null, false);
        Boolean hasActiveFlow = UtilValidate.isNotEmpty(mainProcess);
        BigDecimal configFieldTotal = new BigDecimal(dbFields.size());
        entity.addProperty("hasActiveFlow", hasActiveFlow);
        entity.addProperty("configFieldTotal", configFieldTotal);
    }

    return entityList;
}
