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
public class F extends AbstractEntity {
	private static final long serialVersionUID = 1471534025L;

	/**
	 * No documentation
	 */
	@jakarta.persistence.OneToMany(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, mappedBy = "f")
	private java.util.Set<B> bCollection = new java.util.HashSet<B>();

	/**
	 * No documentation
	 */
	@jakarta.persistence.OneToOne(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	)
	private H h;

	public java.util.Set<B> getBCollection() {
		return bCollection;
	}

	public void setBCollection(
		java.util.Set<B> parameter) {
		this.bCollection = parameter;
	}

	public H getH() {
		return h;
	}

	public void setH(H parameter) {
		this.h = parameter;
	}
}
