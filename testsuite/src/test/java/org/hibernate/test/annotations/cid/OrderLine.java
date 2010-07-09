package org.hibernate.test.annotations.cid;

import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(OrderLinePk.class)
public class OrderLine {
    @Id
    public Order order;
    @Id
    public Product product;
}
