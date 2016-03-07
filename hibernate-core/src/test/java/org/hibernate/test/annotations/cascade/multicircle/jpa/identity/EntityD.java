/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade.multicircle.jpa.identity;

/**
 * No Documentation
 */
@javax.persistence.Entity
public class EntityD extends AbstractEntity {
    private static final long serialVersionUID = 2417176961L;

	@javax.persistence.OneToMany(mappedBy = "d")
	private java.util.Set<EntityB> bCollection = new java.util.HashSet<EntityB>();

	@javax.persistence.ManyToOne(optional = false)
	private EntityC c;

	@javax.persistence.ManyToOne(optional = false)
	private EntityE e;

    @javax.persistence.OneToMany(cascade =  {
        javax.persistence.CascadeType.MERGE,
			javax.persistence.CascadeType.PERSIST,
			javax.persistence.CascadeType.REFRESH},
			mappedBy = "d"
    )
    private java.util.Set<EntityF> fCollection = new java.util.HashSet<EntityF>();

	public java.util.Set<EntityB> getBCollection() {
		return bCollection;
	}
	public void setBCollection(
			java.util.Set<EntityB> parameter) {
		this.bCollection = parameter;
	}

	public EntityC getC() {
		return c;
	}
	public void setC(EntityC c) {
		this.c = c;
	}

	public EntityE getE() {
		return e;
	}
	public void setE(EntityE e) {
		this.e = e;
	}

    public java.util.Set<EntityF> getFCollection() {
        return fCollection;
    }
    public void setFCollection(
			java.util.Set<EntityF> parameter) {
        this.fCollection = parameter;
    }

}
