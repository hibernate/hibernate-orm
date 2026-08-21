/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.entity;


import org.hibernate.annotations.NamedEntityGraph;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "BadRootClassEntity")
@Table(name = "BadRootClassEntity")
@NamedEntityGraph(root = Book.class, name = "bad-root", graph = "name")
public class BadRootClassEntity {
	@Id
	private Integer id;
	private String name;
}
