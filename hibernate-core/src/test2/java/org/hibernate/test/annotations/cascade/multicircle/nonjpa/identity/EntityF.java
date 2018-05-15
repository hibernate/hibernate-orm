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
public class EntityF extends AbstractEntity {
    private static final long serialVersionUID = 1471534025L;

    /**
     * No documentation
     */
    @javax.persistence.OneToMany(mappedBy = "f")
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.SAVE_UPDATE,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
    private java.util.Set<EntityE> eCollection = new java.util.HashSet<EntityE>();

	@javax.persistence.ManyToOne(optional = false)
	private EntityD d;

	@javax.persistence.ManyToOne(optional = false)
	private EntityG g;

    public java.util.Set<EntityE> getECollection() {
        return eCollection;
    }
    public void setECollection(
        java.util.Set<EntityE> parameter) {
        this.eCollection = parameter;
    }

    public EntityD getD() {
        return d;
    }
    public void setD(EntityD parameter) {
        this.d = parameter;
    }

	public EntityG getG() {
		return g;
	}
	public void setG(EntityG parameter) {
		this.g = parameter;
	}

}
