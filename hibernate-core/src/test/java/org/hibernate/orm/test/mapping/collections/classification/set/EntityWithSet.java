/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections.classification.set; /**
 * @author Steve Ebersole
 */

import java.util.Set;

import org.hibernate.orm.test.mapping.collections.classification.Name;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

//tag::collections-set-ex[]
@Entity
public class EntityWithSet {
	// ...
//end::collections-set-ex[]

	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::collections-set-ex[]
	@ElementCollection
	private Set<Name> names;
	//end::collections-set-ex[]

	private EntityWithSet() {
		// for Hibernate use
	}

	public EntityWithSet(Integer id, String name) {
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

//tag::collections-set-ex[]
}
//end::collections-set-ex[]
