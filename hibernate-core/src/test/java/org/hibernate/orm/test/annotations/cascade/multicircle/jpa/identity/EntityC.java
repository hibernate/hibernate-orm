/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.jpa.identity;

import java.util.Set;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class EntityC extends AbstractEntity {
	private static final long serialVersionUID = 1226955752L;

	@jakarta.persistence.OneToMany(mappedBy = "c")
	private Set<EntityB> bCollection = new java.util.HashSet<EntityB>();

	@jakarta.persistence.OneToMany(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, mappedBy = "c")
	private Set<EntityD> dCollection = new java.util.HashSet<EntityD>();

	public Set<EntityB> getBCollection() {
		return bCollection;
	}

	public void setBCollection(Set<EntityB> bCollection) {
		this.bCollection = bCollection;
	}

	public Set<EntityD> getDCollection() {
		return dCollection;
	}

	public void setDCollection(Set<EntityD> dCollection) {
		this.dCollection = dCollection;
	}

}
