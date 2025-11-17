/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.NamedEntityGraph;

/**
 * @author Steve Ebersole
 */
@Entity(name = "InvalidParsedGraphEntity")
@Table(name = "InvalidParsedGraphEntity")
@NamedEntityGraph(name = "invalid", graph = "InvalidParsedGraphEntity: name")
public class InvalidParsedGraphEntity {
	@Id
	private Integer id;
	private String name;
}
