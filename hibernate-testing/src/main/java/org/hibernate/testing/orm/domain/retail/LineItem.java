/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.retail;

import javax.money.MonetaryAmount;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
