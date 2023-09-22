package com.banfftech.factory;

import com.banfftech.bean.TreeNode;
import com.dpbird.odata.services.OfbizServiceException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;

/**
 * 处理抄送节点
 *
 * @author scy
 * @date 2023/9/22
 */
public class ExecMakeCopy implements ExecNode {

    @Override
    public void exec(Delegator delegator, TreeNode nextNode, GenericValue parentWorkEffort) throws OfbizServiceException {
        //TODO: 抄送节点
    }
}
