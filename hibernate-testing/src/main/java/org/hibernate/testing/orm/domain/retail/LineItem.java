/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.domain.retail;

import javax.money.MonetaryAmount;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class LineItem {
	private Integer id;
	private Product product;

	private int quantity;
	private MonetaryAmount subTotal;

	private Order order;

	public LineItem() {
	}

	public LineItem(
			Integer id,
			Product product,
			int quantity,
			MonetaryAmount subTotal,
			Order order) {
		this.id = id;
		this.product = product;
		this.quantity = quantity;
		this.subTotal = subTotal;
		this.order = order;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn( name = "product_id" )
	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public MonetaryAmount getSubTotal() {
		return subTotal;
	}

	public void setSubTotal(MonetaryAmount subTotal) {
		this.subTotal = subTotal;
	}

	@ManyToOne
	@JoinColumn( name = "order_id" )
	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}
}
