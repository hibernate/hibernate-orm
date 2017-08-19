package org.hibernate.jpamodelgen.test.collectiontype;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

/**
 * Created by helloztt on 2017-08-19.
 */
@Entity
public class Goods {
    @Id
    private Long id;
    private List<ProductDisRebateDesc> productRebateConfigs;
    private List<String> stringList;
}
