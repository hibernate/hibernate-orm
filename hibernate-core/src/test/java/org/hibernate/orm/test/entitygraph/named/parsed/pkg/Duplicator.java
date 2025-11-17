/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed.pkg;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import org.hibernate.annotations.NamedEntityGraph;

/**
 * @author Steve Ebersole
 */
@Entity
@NamedEntityGraph( name = "duplicated-name", graph = "name")
public class Duplicator {
	@Id
	private Integer id;
	@Basic
	private String name;

	protected Duplicator() {
		// for Hibernate use
	}

	public Duplicator(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
