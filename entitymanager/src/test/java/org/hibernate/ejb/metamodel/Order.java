/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.metamodel;

import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "ORDER_TABLE")
public class Order implements java.io.Serializable {
	private String id;
	private double totalPrice;
	private Customer customer;
	private CreditCard creditCard;
	private LineItem sampleLineItem;
	private Collection<LineItem> lineItems = new java.util.ArrayList<LineItem>();

	public Order() {
	}

	public Order(String id, double totalPrice) {
		this.id = id;
		this.totalPrice = totalPrice;
	}

	public Order(String id, Customer customer) {
		this.id = id;
		this.customer = customer;
	}

	public Order(String id) {
		this.id = id;
	}

	//====================================================================
	// getters and setters for State fields

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name = "TOTALPRICE")
	public double getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(double price) {
		this.totalPrice = price;
	}

	//====================================================================
	// getters and setters for Association fields

	// MANYx1

	@ManyToOne
	@JoinColumn(
			name = "FK4_FOR_CUSTOMER_TABLE")
	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	//1x1

	@OneToOne(mappedBy = "order")
	public CreditCard getCreditCard() {
		return creditCard;
	}

	public void setCreditCard(CreditCard cc) {
		this.creditCard = cc;
	}

	// 1x1

	@OneToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(
			name = "FK0_FOR_LINEITEM_TABLE")
	public LineItem getSampleLineItem() {
		return sampleLineItem;
	}

	public void setSampleLineItem(LineItem l) {
		this.sampleLineItem = l;
	}

	//1xMANY

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "order")
	public Collection<LineItem> getLineItems() {
		return lineItems;
	}

	public void setLineItems(Collection<LineItem> c) {
		this.lineItems = c;
	}
}