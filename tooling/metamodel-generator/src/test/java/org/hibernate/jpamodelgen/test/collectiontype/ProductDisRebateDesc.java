package org.hibernate.jpamodelgen.test.collectiontype;

import java.util.List;

/**
 * 某个货品设定的分校返利金额,含是否是个性化的返利设置
 * Created by allan on 2/1/16.
 */
public class ProductDisRebateDesc {
    /**
     * 货品Id
     */
    private int proId;
    /**
     * 返利金额，IsCustom=0时有效
     */
    private double amount;
    /**
     * 是否个性化设置货品返利
     */
    private int isCustom;
}
