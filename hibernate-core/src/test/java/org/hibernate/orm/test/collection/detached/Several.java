/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.detached;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.Set;

@Entity @Table(name="DCSeveral")
public class Several {
	@GeneratedValue
	@Id
	long id;

	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	Set<Many> many;
}
