/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SecondaryTable;

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
