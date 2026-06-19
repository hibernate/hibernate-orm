/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping.compositeelement;

import java.util.Set;

public class MyEntity {
	private Long id;
	private String name;
	private Set addresses;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set getAddresses() {
		return addresses;
	}

	public void setAddresses(Set addresses) {
		this.addresses = addresses;
	}
}
