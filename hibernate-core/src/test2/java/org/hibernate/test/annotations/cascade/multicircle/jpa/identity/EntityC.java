/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade.multicircle.jpa.identity;

import java.util.Set;

/**
 * No Documentation
 */
@javax.persistence.Entity
public class EntityC extends AbstractEntity {
    private static final long serialVersionUID = 1226955752L;

	@javax.persistence.OneToMany(mappedBy = "c")
	private Set<EntityB> bCollection = new java.util.HashSet<EntityB>();

	@javax.persistence.OneToMany(cascade =  {
		javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
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
