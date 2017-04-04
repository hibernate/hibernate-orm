/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade.multicircle.nonjpa.identity;

@javax.persistence.Entity
public class EntityB extends AbstractEntity {
    private static final long serialVersionUID = 325417243L;

    @javax.persistence.OneToMany(mappedBy = "b")
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.SAVE_UPDATE,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
    private java.util.Set<EntityG> gCollection = new java.util.HashSet<EntityG>();


	@javax.persistence.ManyToOne(optional = false)
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.SAVE_UPDATE,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
	private EntityC c;

	@javax.persistence.ManyToOne(optional = false)
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.SAVE_UPDATE,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
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
