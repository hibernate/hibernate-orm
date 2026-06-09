/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associationmanagement;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Person")
class Person {
	@Id
	Integer id;

	@OneToOne(mappedBy = "person", fetch = FetchType.LAZY)
	Passport passport;

	Person() {
	}

	Person(Integer id) {
		this.id = id;
	}
}
