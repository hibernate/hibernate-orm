/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

public class FormulaEmployee {
	private Long id;
	private FormulaPerson person;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public FormulaPerson getPerson() {
		return person;
	}

	public void setPerson(FormulaPerson person) {
		this.person = person;
	}
}
