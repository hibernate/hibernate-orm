/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.cascade.multicircle;

/**
 * No Documentation
 */
@javax.persistence.Entity
public class E extends AbstractEntity {
    private static final long serialVersionUID = 1226955558L;

	@javax.persistence.OneToMany(mappedBy = "e")
	private java.util.Set<D> dCollection = new java.util.HashSet<D>();

	@javax.persistence.ManyToOne(optional = true)
	private F f;

	public java.util.Set<D> getDCollection() {
		return dCollection;
	}
	public void setDCollection(java.util.Set<D> dCollection) {
		this.dCollection = dCollection;
	}

    public F getF() {
        return f;
    }
    public void setF(F parameter) {
        this.f = parameter;
    }
}
