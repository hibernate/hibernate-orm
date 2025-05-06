/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subclassfilter;


/**
 * Implementation of Customer.
 *
 * @author Steve Ebersole
 */
public class Customer extends Person {
	private Employee contactOwner;

	public Customer() {
	}

	public Customer(String name) {
		super( name );
	}

	public Employee getContactOwner() {
		return contactOwner;
	}

	public void setContactOwner(Employee contactOwner) {
		this.contactOwner = contactOwner;
	}
}
