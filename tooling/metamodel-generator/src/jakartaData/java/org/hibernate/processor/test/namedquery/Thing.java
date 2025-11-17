/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.namedquery;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;

@NamedQuery(name = "#things",
		query = "from Thing where name like :name")
@Entity
public class Thing {
	@Id @GeneratedValue long id;
	String name;
}
