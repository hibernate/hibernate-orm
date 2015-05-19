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
public class B extends AbstractEntity {
    private static final long serialVersionUID = 325417243L;

    /**
     * No documentation
     */
    @javax.persistence.OneToMany(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    , mappedBy = "b")
    private java.util.Set<G> gCollection = new java.util.HashSet<G>();


	/**
	 * No documentation
	 */
	@javax.persistence.ManyToOne(cascade =  {
		javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
	, optional = false)
	private C c;



    /**
     * No documentation
     */
    @javax.persistence.ManyToOne(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    , optional = false)
    private D d;

    public java.util.Set<G> getGCollection() {
        return gCollection;
    }

    public void setGCollection(
        java.util.Set<G> parameter) {
        this.gCollection = parameter;
    }

    public C getC() {
        return c;
    }

    public void setC(C parameter) {
        this.c = parameter;
    }

	public D getD() {
		return d;
	}

	public void setD(D parameter) {
		this.d = parameter;
	}

}
