package com.banfftech.factory;

import com.banfftech.bean.TreeNode;
import com.dpbird.odata.services.OfbizServiceException;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;

/**
 * @author scy
 * @date 2023/9/22
 */
public interface ExecNode {

    /**
     * 执行节点
     */
    void exec(Delegator delegator, TreeNode nextNode, GenericValue parentWorkEffort) throws OfbizServiceException;

}
