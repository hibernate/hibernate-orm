/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.jpa.compliance.callback.listeneroverrides;

import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "orders")
@EntityListeners({ListenerC.class, ListenerB.class})
public class Order extends CallbackTarget {
	@Id
	private Integer id;
	@Column(name = "total_price")
	private double totalPrice;
	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
	private Collection<LineItem> lineItems = new java.util.ArrayList<>();

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
