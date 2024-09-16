/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cascade.circle.sequence;

/**
 * No Documentation
 */
@jakarta.persistence.Entity
public class D extends AbstractEntity {
	private static final long serialVersionUID = 2417176961L;

	/**
	 * No documentation
	 */
	@jakarta.persistence.ManyToMany(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	)
	private java.util.Set<A> aCollection = new java.util.HashSet<A>();

	/**
	 * No documentation
	 */
	@jakarta.persistence.OneToMany(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	)
	private java.util.Set<E> eCollection = new java.util.HashSet<E>();

	public java.util.Set<A> getACollection() {
		return aCollection;
	}

	public void setACollection(
		java.util.Set<A> parameter) {
		this.aCollection = parameter;
	}

	public java.util.Set<E> getECollection() {
		return eCollection;
	}

	public void setECollection(
		java.util.Set<E> parameter) {
		this.eCollection = parameter;
	}
}
