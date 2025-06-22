/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "ForeignCustomer")
@Table(name = "foreign_cust")
public class ForeignCustomer extends Customer {
	private String vat;

	public ForeignCustomer() {
	}

	public ForeignCustomer(
			Integer oid,
			String name,
			Address address,
			String vat,
			Customer parentCustomer) {
		super( oid, name, address, parentCustomer );
		this.vat = vat;
	}

	public ForeignCustomer(
			Integer oid,
			String name,
			Address address,
			String vat) {
		this( oid, name, address, vat, null );
	}

	public String getVat() {
		return vat;
	}

	public void setVat(String vat) {
		this.vat = vat;
	}
}
