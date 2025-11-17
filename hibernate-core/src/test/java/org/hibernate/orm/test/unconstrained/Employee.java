/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unconstrained;


/**
 * @author Gavin King
 */
public class Employee {

	private String id;

	public Employee() {
	}

	public Employee(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


}
