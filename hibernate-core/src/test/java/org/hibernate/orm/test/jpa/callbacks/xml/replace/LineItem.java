/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml.replace;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@Entity
@EntityListeners({ ListenerC.class, ListenerB.class})
@ExcludeDefaultListeners()
@ExcludeSuperclassListeners
public class LineItem extends LineItemSuper {
	@Id
	private Integer id;
	@ManyToOne
	@JoinColumn(name = "order_fk")
	private Order order;
	@ManyToOne
	@JoinColumn(name = "product_fk")
	private Product product;

	public LineItem() {
	}

	public LineItem(Integer id, Order order, Product product, int quantity) {
		super( quantity );
		this.id = id;
		this.order = order;
		this.product = product;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}
}
