/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.collections.classification.explicit;

import java.util.Collection;

import org.hibernate.annotations.CollectionClassificationType;
import org.hibernate.orm.test.mapping.collections.classification.Name;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hibernate.metamodel.CollectionClassification.SET;

/**
 * @author Steve Ebersole
 */
//tag::collections-bag-set-ex[]
@Entity
public class EntityWithExplicitSetClassification {
	// ...
//end::collections-bag-set-ex[]

	@Id
	private Integer id;
	@Basic
	private String name;

	//tag::collections-bag-set-ex[]
	@ElementCollection
	@CollectionClassificationType(SET)
	private Collection<Name> names;
	//end::collections-bag-set-ex[]

	private EntityWithExplicitSetClassification() {
		// for Hibernate use
	}

	public EntityWithExplicitSetClassification(Integer id, String name) {
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
//tag::collections-bag-set-ex[]
}
//end::collections-bag-set-ex[]
