/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections.classification.set;

import java.util.SortedSet;

import org.hibernate.annotations.SortNatural;
import org.hibernate.orm.test.mapping.collections.classification.Name;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
//tag::collections-sortedset-natural-ex[]
@Entity
public class EntityWithNaturallySortedSet {
	// ...
//end::collections-sortedset-natural-ex[]

	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::collections-sortedset-natural-ex[]
	@ElementCollection
	@SortNatural
	private SortedSet<Name> names;
	//end::collections-sortedset-natural-ex[]

	private EntityWithNaturallySortedSet() {
		// for Hibernate use
	}

	public EntityWithNaturallySortedSet(Integer id, String name) {
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
//tag::collections-sortedset-natural-ex[]
}
//end::collections-sortedset-natural-ex[]
