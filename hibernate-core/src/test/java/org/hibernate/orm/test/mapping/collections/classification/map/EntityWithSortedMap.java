/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.classification.map; /**
 * @author Steve Ebersole
 */

import java.util.Map;

import org.hibernate.annotations.SortComparator;
import org.hibernate.orm.test.mapping.collections.classification.Name;
import org.hibernate.orm.test.mapping.collections.classification.NameComparator;
import org.hibernate.orm.test.mapping.collections.classification.Status;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

//tag::example[]
@Entity
public class EntityWithSortedMap {
	// ...
//end::example[]

	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::example[]
	@ElementCollection
	@SortComparator( NameComparator.class )
	private Map<Name, Status> names;
	//end::example[]

	private EntityWithSortedMap() {
		// for Hibernate use
	}

	public EntityWithSortedMap(Integer id, String name) {
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
//tag::example[]
}
//end::example[]
