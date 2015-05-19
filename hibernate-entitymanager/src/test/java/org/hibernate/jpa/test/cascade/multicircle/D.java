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
public class D extends AbstractEntity {
    private static final long serialVersionUID = 2417176961L;

	@javax.persistence.OneToMany(mappedBy = "d")
	private java.util.Set<B> bCollection = new java.util.HashSet<B>();

	@javax.persistence.ManyToOne(optional = false)
	private C c;

	@javax.persistence.ManyToOne(optional = false)
	private E e;

    @javax.persistence.OneToMany(cascade =  {
        javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.REFRESH},
			mappedBy = "d"
    )
    private java.util.Set<F> fCollection = new java.util.HashSet<F>();

	public java.util.Set<B> getBCollection() {
		return bCollection;
	}
	public void setBCollection(
			java.util.Set<B> parameter) {
		this.bCollection = parameter;
	}

	public C getC() {
		return c;
	}
	public void setC(C c) {
		this.c = c;
	}

	public E getE() {
		return e;
	}
	public void setE(E e) {
		this.e = e;
	}

    public java.util.Set<F> getFCollection() {
        return fCollection;
    }
    public void setFCollection(
			java.util.Set<F> parameter) {
        this.fCollection = parameter;
    }

}
