/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade.multicircle.jpa.sequence;

/**
 * No Documentation
 */
@javax.persistence.Entity
public class F extends AbstractEntity {
    private static final long serialVersionUID = 1471534025L;

    /**
     * No documentation
     */
    @javax.persistence.OneToMany(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH}
    , mappedBy = "f")
    private java.util.Set<E> eCollection = new java.util.HashSet<E>();

	@javax.persistence.ManyToOne(optional = false)
	private D d;

	@javax.persistence.ManyToOne(optional = false)
	private G g;

    public java.util.Set<E> getECollection() {
        return eCollection;
    }
    public void setECollection(
        java.util.Set<E> parameter) {
        this.eCollection = parameter;
    }

    public D getD() {
        return d;
    }
    public void setD(D parameter) {
        this.d = parameter;
    }

	public G getG() {
		return g;
	}
	public void setG(G parameter) {
		this.g = parameter;
	}

}
