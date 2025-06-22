/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.original;


/**
 * @author Gavin King
 */
public class Email {
	private String address;
	Email() {}
	public String getAddress() {
		return address;
	}
	public void setAddress(String type) {
		this.address = type;
	}
	public Email(String type) {
		this.address = type;
	}
	public boolean equals(Object that) {
		if ( !(that instanceof Email) ) return false;
		Email p = (Email) that;
		return this.address.equals(p.address);
	}
	public int hashCode() {
		return address.hashCode();
	}
}
