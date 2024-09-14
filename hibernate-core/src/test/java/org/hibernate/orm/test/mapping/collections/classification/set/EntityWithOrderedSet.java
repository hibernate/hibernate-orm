/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
