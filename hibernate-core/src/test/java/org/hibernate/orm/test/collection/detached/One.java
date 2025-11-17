/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.detached;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

@Entity @Table(name="DCOne")
public class One {
	@GeneratedValue
	@Id
	long id;

	@OneToMany(mappedBy = "one",
			cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	List<Many> many;

	public long getId() {
		return id;
	}
}
