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
public class EntityB extends AbstractEntity {
    private static final long serialVersionUID = 325417243L;

    /**
     * No documentation
     */
    @javax.persistence.OneToMany(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    , mappedBy = "b")
    private java.util.Set<EntityG> gCollection = new java.util.HashSet<EntityG>();


	/**
	 * No documentation
	 */
	@javax.persistence.ManyToOne(cascade =  {
		javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
	, optional = false)
	private EntityC c;



    /**
     * No documentation
     */
    @javax.persistence.ManyToOne(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
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
