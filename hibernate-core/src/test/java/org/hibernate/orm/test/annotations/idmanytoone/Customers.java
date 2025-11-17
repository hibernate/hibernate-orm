/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="Customers")
public class Customers implements Serializable {

	private static final long serialVersionUID = -885167444315163039L;

	@Column(name="customerID", nullable=false)
	@Id
	private int customerID;

	@OneToMany(mappedBy="owner", cascade= CascadeType.ALL, targetEntity=ShoppingBaskets.class)
	private java.util.Set<ShoppingBaskets> shoppingBasketses = new java.util.HashSet<>();

	public void setCustomerID(int value) {
		this.customerID = value;
	}

	public int getCustomerID() {
		return customerID;
	}

	public int getORMID() {
		return getCustomerID();
	}

	public void setShoppingBasketses(java.util.Set<ShoppingBaskets> value) {
		this.shoppingBasketses = value;
	}

	public java.util.Set<ShoppingBaskets> getShoppingBasketses() {
		return shoppingBasketses;
	}

}
