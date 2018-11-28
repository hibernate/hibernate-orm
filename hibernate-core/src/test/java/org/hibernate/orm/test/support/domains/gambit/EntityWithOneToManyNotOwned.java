/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.support.domains.gambit;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Chris Cranford
 */
@Entity
public class EntityWithOneToManyNotOwned {
	private Integer id;
	private List<EntityWithManyToOneNonJoinTable> collection = new ArrayList<>();

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany(mappedBy = "owner")
	public List<EntityWithManyToOneNonJoinTable> getCollection() {
		return collection;
	}

	public void setCollection(List<EntityWithManyToOneNonJoinTable> collection) {
		this.collection = collection;
	}

	public void addChild(EntityWithManyToOneNonJoinTable child) {
		child.setOwner( this );
		getCollection().add( child );
	}
}
