/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Name {
	private String first;
	private String last;

	private Name() {
	}

	public Name(String first, String last) {
		this.first = first;
		this.last = last;
	}

	@Column(name = "first_name")
	public String getFirstName() {
		return first;
	}

	public void setFirstName(String first) {
		this.first = first;
	}

	@Column(name = "last_name")
	public String getLastName() {
		return last;
	}

	public void setLastName(String last) {
		this.last = last;
	}
}
