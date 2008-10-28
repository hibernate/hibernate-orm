package org.hibernate.test.annotations.cid;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "OrderTableFoobar")
public class Order {
	@Id
    @GeneratedValue
    public Integer id;
}
