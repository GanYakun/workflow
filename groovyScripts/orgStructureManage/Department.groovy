import com.dpbird.odata.edm.OdataOfbizEntity;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilDateTime;
import java.sql.Timestamp;

module = "generateFields.groovy"

def generateFields(Map<String, Object> context) {
    List<OdataOfbizEntity> entityList = context.parameters.entityList;
    entityList.each { entity ->
        GenericValue department = (GenericValue) entity.getGenericValue();

//        String partyId = department.getString("partyName");
//        EntityQuery.use(delegator).from("PartyRelationShip")
//                .where("partyIdFrom", partyId, "roleTypeIdTo", "MEMBER").queryOne();

        entity.addProperty("memberNumber", 10);
    }

    return entityList;
}
