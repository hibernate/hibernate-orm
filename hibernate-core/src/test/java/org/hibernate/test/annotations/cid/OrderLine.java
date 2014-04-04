package org.hibernate.test.annotations.cid;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(OrderLinePk.class)
public class OrderLine {
    @Id
	@ManyToOne
    public Order order;
    @Id
	@ManyToOne
    public Product product;
}
