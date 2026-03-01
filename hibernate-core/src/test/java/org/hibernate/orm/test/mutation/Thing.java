/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mutation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity(name = "Thing")
@Table(name = "stored_thing")
@NamedQuery(
		name = "Thing.byName",
		query = "select e from Thing e where e.name like :name"
)
class Thing {
	@Id
	Long id;

	@Version
	int version;

	String name;

	Thing() {
	}

	Thing(Long id, String name) {
		this.id = id;
		this.name = name;
	}
}
