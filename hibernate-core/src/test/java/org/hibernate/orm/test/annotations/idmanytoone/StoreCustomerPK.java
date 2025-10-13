/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class StoreCustomerPK implements Serializable {
	StoreCustomerPK() {
	}

	@Id
	@ManyToOne(optional = false)
	@JoinColumn(name = "idA")
	public Store store;

	@Id
	@ManyToOne(optional = false)
	@JoinColumn(name = "idB")
	public Customer customer;


	public StoreCustomerPK(Store store, Customer customer) {
		this.store = store;
		this.customer = customer;
	}


	private static final long serialVersionUID = -1102111921432271459L;
}
