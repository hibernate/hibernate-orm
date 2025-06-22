/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.classification.list;/**
 * @author Steve Ebersole
 */

import java.util.List;

import org.hibernate.orm.test.mapping.collections.classification.Name;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */ //tag::collections-list-ex[]
@Entity
public class EntityWithList {
	// ...
//end::collections-list-ex[]

	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::collections-list-ex[]
	@ElementCollection
	private List<Name> names;
	//end::collections-list-ex[]

	private EntityWithList() {
		// for Hibernate use
	}

	public EntityWithList(Integer id, String name) {
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

	//tag::collections-list-ex[]
}
//end::collections-list-ex[]
