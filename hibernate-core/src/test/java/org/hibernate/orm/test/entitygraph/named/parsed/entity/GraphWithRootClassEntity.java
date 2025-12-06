/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.entity;

import org.hibernate.annotations.NamedEntityGraph;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity(name = "GraphWithRootClassEntity")
@Table(name = "GraphWithRootClassEntity")
@NamedEntityGraph(root = GraphWithRootClassEntity.class, name = "valid-root-on-annotation", graph = "name")
public class GraphWithRootClassEntity {
	@Id
	private Integer id;
	private String name;
}
