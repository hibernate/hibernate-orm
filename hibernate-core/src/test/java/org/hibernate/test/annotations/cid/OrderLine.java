package org.hibernate.test.annotations.cid;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

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
