/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.joincolumnfk;

import java.io.Serializable;
import java.util.Map;

public class Company implements Serializable {

	private int id;
	private String name;
	private Map<Division, VicePresident> organization;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<Division, VicePresident> getOrganization() {
		return organization;
	}

	public void setOrganization(Map<Division, VicePresident> organization) {
		this.organization = organization;
	}
}
