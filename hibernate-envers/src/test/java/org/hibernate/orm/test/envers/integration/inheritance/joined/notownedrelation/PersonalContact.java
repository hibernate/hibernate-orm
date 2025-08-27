/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.notownedrelation;

import jakarta.persistence.Entity;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class PersonalContact extends Contact {
	private String firstname;

	public PersonalContact() {
	}

	public PersonalContact(Long id, String email, String firstname) {
		super( id, email );
		this.firstname = firstname;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}
}
