/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade.multicircle.nonjpa.identity;

/**
 * No Documentation
 */
@javax.persistence.Entity
public class EntityE extends AbstractEntity {
    private static final long serialVersionUID = 1226955558L;

	@javax.persistence.OneToMany(mappedBy = "e")
	private java.util.Set<EntityD> dCollection = new java.util.HashSet<EntityD>();

	@javax.persistence.ManyToOne(optional = true)
	private EntityF f;

	public java.util.Set<EntityD> getDCollection() {
		return dCollection;
	}
	public void setDCollection(java.util.Set<EntityD> dCollection) {
		this.dCollection = dCollection;
	}

    public EntityF getF() {
        return f;
    }
    public void setF(EntityF parameter) {
        this.f = parameter;
    }
}
