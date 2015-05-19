/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade.multicircle.nonjpa.identity;

@javax.persistence.Entity
public class B extends AbstractEntity {
    private static final long serialVersionUID = 325417243L;

    @javax.persistence.OneToMany(mappedBy = "b")
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.SAVE_UPDATE,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
    private java.util.Set<G> gCollection = new java.util.HashSet<G>();


	@javax.persistence.ManyToOne(optional = false)
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.SAVE_UPDATE,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
	private C c;

	@javax.persistence.ManyToOne(optional = false)
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.SAVE_UPDATE,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
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
