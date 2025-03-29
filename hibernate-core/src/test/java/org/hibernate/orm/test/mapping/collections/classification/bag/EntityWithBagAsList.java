/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.classification.bag;

import java.util.List;

import org.hibernate.annotations.Bag;
import org.hibernate.orm.test.mapping.collections.classification.Name;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
//tag::collections-bag-list-ex[]
@Entity
public class EntityWithBagAsList {
	// ..
//end::collections-bag-list-ex[]

	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::collections-bag-list-ex[]
	@ElementCollection
	@Bag
	private List<Name> names;
	//end::collections-bag-list-ex[]

	private EntityWithBagAsList() {
		// for Hibernate use
	}

	public EntityWithBagAsList(Integer id, String name) {
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
//tag::collections-bag-list-ex[]
}
//end::collections-bag-list-ex[]
