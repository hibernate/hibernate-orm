/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import org.hibernate.annotations.NamedEntityGraph;

/**
 * @author Steve Ebersole
 */
@NamedEntityGraph(name = "test-id-name", graph = "(id, name)")
@jakarta.persistence.NamedEntityGraph(
		name = "test-id-name",
		attributeNodes = {
				@NamedAttributeNode("id"),
				@NamedAttributeNode("name")
		}
)
@Entity
public class Duplicator {
	@Id
	private Integer id;
	private String name;
}
