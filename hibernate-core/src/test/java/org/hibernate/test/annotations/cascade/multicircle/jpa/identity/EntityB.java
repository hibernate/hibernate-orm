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
public class EntityB extends AbstractEntity {
    private static final long serialVersionUID = 325417243L;

    /**
     * No documentation
     */
    @jakarta.persistence.OneToMany(cascade =  {
        jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
    , mappedBy = "b")
    private java.util.Set<EntityG> gCollection = new java.util.HashSet<EntityG>();


	/**
	 * No documentation
	 */
	@jakarta.persistence.ManyToOne(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, optional = false)
	private EntityC c;



    /**
     * No documentation
     */
    @jakarta.persistence.ManyToOne(cascade =  {
        jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
    , optional = false)
    private EntityD d;

    public java.util.Set<EntityG> getGCollection() {
        return gCollection;
    }

    public void setGCollection(
        java.util.Set<EntityG> parameter) {
        this.gCollection = parameter;
    }

    public EntityC getC() {
        return c;
    }

    public void setC(EntityC parameter) {
        this.c = parameter;
    }

	public EntityD getD() {
		return d;
	}

	public void setD(EntityD parameter) {
		this.d = parameter;
	}

}
