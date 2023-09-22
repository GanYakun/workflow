package com.banfftech.factory;

/**
 * ExecNode factory
 */
public class ExecFactory {

    /**
     * 获取一个执行节点的实例
     */
    public static ExecNode getExecNode(int nodeType) {
        if (nodeType == 1) {
            return new ExecApproval();
        }
        if (nodeType == 2) {
            return new ExecMakeCopy();
        }
        if (nodeType == 4) {
            return new ExecRouting();
        }
        return null;
    }

}
