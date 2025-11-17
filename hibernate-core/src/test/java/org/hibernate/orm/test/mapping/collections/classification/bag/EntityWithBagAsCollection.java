/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.classification.bag;

import java.util.Collection;

import org.hibernate.orm.test.mapping.collections.classification.Name;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
//tag::collections-bag-ex[]
@Entity
public class EntityWithBagAsCollection {
	// ..
//end::collections-bag-ex[]

	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::collections-bag-ex[]
	@ElementCollection
	private Collection<Name> names;
	//end::collections-bag-ex[]

	private EntityWithBagAsCollection() {
		// for Hibernate use
	}

	public EntityWithBagAsCollection(Integer id, String name) {
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
//tag::collections-bag-ex[]
}
//end::collections-bag-ex[]
