/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml.complete;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.orm.test.jpa.callbacks.xml.common.CallbackTarget;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "orders")
public class Order extends CallbackTarget {
	@Id
	private Integer id;
	@Column(name = "total_price")
	private double totalPrice;
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
	private Collection<LineItem> lineItems = new ArrayList<>();

	public Order() {
	}

	public Order(Integer id, double totalPrice) {
		this.id = id;
		this.totalPrice = totalPrice;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public double getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(double totalPrice) {
		this.totalPrice = totalPrice;
	}

	public Collection<LineItem> getLineItems() {
		return lineItems;
	}

	public void setLineItems(Collection<LineItem> lineItems) {
		this.lineItems = lineItems;
	}

	public void addLineItem(LineItem lineItem) {
		if ( lineItems == null ) {
			lineItems = new ArrayList<>();
		}
		lineItems.add( lineItem );
	}
}
