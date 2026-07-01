/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

public class FormulaUser {
	private String userName;
	private FormulaPerson person;

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public FormulaPerson getPerson() {
		return person;
	}

	public void setPerson(FormulaPerson person) {
		this.person = person;
	}
}
