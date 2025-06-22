/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "`ABs`")
@IdClass( StoreCustomerPK.class)
public class StoreCustomer implements Serializable {
	StoreCustomer() {}
	@Id
	@ManyToOne(optional = false)
	@JoinColumn(name = "idA")
	public Store store;

	@Id
	@ManyToOne(optional = false)
	@JoinColumn(name = "idB")
	public Customer customer;


	public StoreCustomer(Store store, Customer customer) {
	this.store = store;
	this.customer = customer;
	}


	private static final long serialVersionUID = -8295955012787627232L;
}
