/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import java.util.List;

public class Customer {

	private Long id;
	private String name;
	List<CustomerInventory> inventory;

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

	public List<CustomerInventory> getInventory() {
		return inventory;
	}

	public void setInventory(List<CustomerInventory> inventory) {
		this.inventory = inventory;
	}
}
