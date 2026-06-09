/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associationmanagement;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Passport")
class Passport {
	@Id
	Integer id;

	@OneToOne
	@JoinColumn(name = "person_id")
	Person person;

	Passport() {
	}

	Passport(Integer id, Person person) {
		this.id = id;
		this.person = person;
	}

	void setPerson(Person person) {
		this.person = person;
	}
}
