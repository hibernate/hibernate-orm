//$Id$
package org.hibernate.test.annotations.manytoone;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class OrderLine {
	private Integer id;
	private String item;
	private Order order;

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
}
