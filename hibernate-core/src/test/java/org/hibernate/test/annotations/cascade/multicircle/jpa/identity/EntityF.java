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
@jakarta.persistence.Entity
public class EntityF extends AbstractEntity {
    private static final long serialVersionUID = 1471534025L;

    /**
     * No documentation
     */
    @jakarta.persistence.OneToMany(cascade =  {
        jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
    , mappedBy = "f")
    private java.util.Set<EntityE> eCollection = new java.util.HashSet<EntityE>();

	@jakarta.persistence.ManyToOne(optional = false)
	private EntityD d;

	@jakarta.persistence.ManyToOne(optional = false)
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
