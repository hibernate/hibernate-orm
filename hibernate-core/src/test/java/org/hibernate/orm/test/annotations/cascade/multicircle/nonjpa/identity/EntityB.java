/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cascade.multicircle.nonjpa.identity;

@jakarta.persistence.Entity
public class EntityB extends AbstractEntity {
	private static final long serialVersionUID = 325417243L;

	@jakarta.persistence.OneToMany(mappedBy = "b")
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
	private java.util.Set<EntityG> gCollection = new java.util.HashSet<EntityG>();


	@jakarta.persistence.ManyToOne(optional = false)
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
			org.hibernate.annotations.CascadeType.MERGE,
			org.hibernate.annotations.CascadeType.REFRESH
	})
	private EntityC c;

	@jakarta.persistence.ManyToOne(optional = false)
	@org.hibernate.annotations.Cascade({
			org.hibernate.annotations.CascadeType.PERSIST,
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
