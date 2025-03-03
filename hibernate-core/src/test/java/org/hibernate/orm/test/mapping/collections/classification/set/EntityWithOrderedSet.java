/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.classification.set;

import java.util.Set;

import org.hibernate.orm.test.mapping.collections.classification.Name;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderBy;

/**
 * @author Steve Ebersole
 */
@Entity
public class EntityWithOrderedSet {
	@Id
	private Integer id;
	@Basic
	private String name;

	@ElementCollection
	@OrderBy( "last" )
	private Set<Name> names;

	private EntityWithOrderedSet() {
		// for Hibernate use
	}

	public EntityWithOrderedSet(Integer id, String name) {
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
