/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.classification.set; /**
 * @author Steve Ebersole
 */

import java.util.SortedSet;

import org.hibernate.annotations.SortComparator;
import org.hibernate.orm.test.mapping.collections.classification.Name;
import org.hibernate.orm.test.mapping.collections.classification.NameComparator;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

//tag::collections-sortedset-comparator-ex[]
@Entity
public class EntityWithSortedSet {
	// ...
//end::collections-sortedset-comparator-ex[]

	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::collections-sortedset-comparator-ex[]
	@ElementCollection
	@SortComparator( NameComparator.class )
	private SortedSet<Name> names;
	//end::collections-sortedset-comparator-ex[]

	private EntityWithSortedSet() {
		// for Hibernate use
	}

	public EntityWithSortedSet(Integer id, String name) {
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
//tag::collections-sortedset-comparator-ex[]
}
//end::collections-sortedset-comparator-ex[]
