/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections.classification.map;

import java.util.Map;

import org.hibernate.annotations.SortNatural;
import org.hibernate.orm.test.mapping.collections.classification.Name;
import org.hibernate.orm.test.mapping.collections.classification.Status;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
//tag::example[]
@Entity
public class EntityWithNaturallySortedMap {
	// ...
//end::example[]

	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::example[]
	@ElementCollection
	@SortNatural
	private Map<Name, Status> names;
	//end::example[]

	private EntityWithNaturallySortedMap() {
		// for Hibernate use
	}

	public EntityWithNaturallySortedMap(Integer id, String name) {
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
