//$Id$
package org.hibernate.test.annotations.manytoone;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SecondaryTable;

/**
 * @author Emmanuel Bernard
 */
@Entity
@SecondaryTable(name="OrderLine_Extension")
public class OrderLine {
	private Integer id;
	private String item;
	private Order order;
	private Order replacementOrder;

	@Id @GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	@ManyToOne
	@JoinColumn(name="order_nbr", referencedColumnName = "order_nbr", unique = true)
	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	@ManyToOne
	@JoinColumn(name="replacement_order_nbr", table="OrderLine_Extension", referencedColumnName = "order_nbr")
	public Order getReplacementOrder() {
		return replacementOrder;
	}

	public void setReplacementOrder(Order replacementOrder) {
		this.replacementOrder = replacementOrder;
	}
}
