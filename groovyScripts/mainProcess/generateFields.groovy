package com.banfftech.handler

import com.dpbird.odata.edm.OdataOfbizEntity
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.util.EntityQuery

module = "generateFields.groovy"

def generateFields(Map<String, Object> context) {
    List<OdataOfbizEntity> entityList = context.parameters.entityList;
    entityList.each { entity ->
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal progressingQuantity = BigDecimal.ZERO;
        BigDecimal completedQuantity = BigDecimal.ZERO;
        GenericValue mainProcess = (GenericValue) entity.getGenericValue();
        GenericValue tempWorkEffort = mainProcess.getRelatedOne("WorkEffort", false)
        if (UtilValidate.isNotEmpty(tempWorkEffort)) {
            //所有数量
            long all = EntityQuery.use(delegator).from("WorkEffort")
                    .where("workEffortParentId", tempWorkEffort.getString("workEffortId")).queryCount();
            totalQuantity = new BigDecimal(all)
            //进行中
            long progressing = EntityQuery.use(delegator).from("WorkEffort")
                    .where("workEffortParentId", tempWorkEffort.getString("workEffortId"), "currentStatusId", "WEPR_WAIT").queryCount();
            progressingQuantity = new BigDecimal(progressing)
            //已完成
            long completed = EntityQuery.use(delegator).from("WorkEffort")
                    .where("workEffortParentId", tempWorkEffort.getString("workEffortId"), "currentStatusId", "WEPR_COMPLETE").queryCount();
            completedQuantity = new BigDecimal(completed)
        }
        entity.addProperty("totalQuantity", totalQuantity);
        entity.addProperty("progressingQuantity", progressingQuantity);
        entity.addProperty("completedQuantity", completedQuantity);
    }

    return entityList;
}
