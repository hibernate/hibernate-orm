/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Map;

@Entity
public class EntityWithMapEC {
	@Id
	private Long id;

	@ElementCollection
	@CollectionTable
	private Map<String, MapElement> elements;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
