/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade.multicircle.nonjpa.identity;

import java.util.Set;

/**
 * No Documentation
 */
@javax.persistence.Entity
public class C extends AbstractEntity {
    private static final long serialVersionUID = 1226955752L;

	@javax.persistence.OneToMany(mappedBy = "c")
	private Set<B> bCollection = new java.util.HashSet<B>();

	@javax.persistence.OneToMany(mappedBy = "c")
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.SAVE_UPDATE,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})

	private Set<D> dCollection = new java.util.HashSet<D>();

	public Set<B> getBCollection() {
		return bCollection;
	}

	public void setBCollection(Set<B> bCollection) {
		this.bCollection = bCollection;
	}

	public Set<D> getDCollection() {
		return dCollection;
	}

	public void setDCollection(Set<D> dCollection) {
		this.dCollection = dCollection;
	}

}
