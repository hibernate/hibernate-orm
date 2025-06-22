/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.access;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;

@Embeddable
public class Name2 {
	@Column(name = "first_name")
	@Convert( converter = SillyConverter.class )
	private String first;
	@Column(name = "last_name")
	private String last;

	private Name2() {
	}

	public Name2(String first, String last) {
		this.first = first;
		this.last = last;
	}

	public String getFirstName() {
		return first;
	}

	public void setFirstName(String first) {
		this.first = first;
	}

	public String getLastName() {
		return last;
	}

	public void setLastName(String last) {
		this.last = last;
	}

}
