package org.hibernate.test.annotations.cid;
import java.io.Serializable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
public class OrderLinePk implements Serializable {
	@ManyToOne
    @JoinColumn(name = "foo", nullable = false)
    public Order order;
	@ManyToOne
    @JoinColumn(name = "bar", nullable = false)
    public Product product;    
}
