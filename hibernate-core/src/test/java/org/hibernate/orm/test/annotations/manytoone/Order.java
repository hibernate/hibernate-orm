/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.manytoone;
import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="ORDERS")
public class Order implements Serializable {
	private Integer id;
	private String orderNbr;
	private Set<OrderLine> orderLines;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name="order_nbr")
	public String getOrderNbr() {
		return orderNbr;
	}

	public void setOrderNbr(String orderNbr) {
		this.orderNbr = orderNbr;
	}

	@OneToMany(mappedBy = "order")
	public Set<OrderLine> getOrderLines() {
		return orderLines;
	}

	public void setOrderLines(Set<OrderLine> orderLines) {
		this.orderLines = orderLines;
	}
}
