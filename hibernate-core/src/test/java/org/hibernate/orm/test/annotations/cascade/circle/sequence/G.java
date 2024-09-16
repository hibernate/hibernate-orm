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
public class G extends AbstractEntity {
	private static final long serialVersionUID = 325417437L;

	/**
	 * No documentation
	 */
	@jakarta.persistence.OneToMany(cascade =  {
		jakarta.persistence.CascadeType.MERGE, jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REFRESH}
	, mappedBy = "g")
	private java.util.Set<C> cCollection = new java.util.HashSet<C>();

	public java.util.Set<C> getCCollection() {
		return cCollection;
	}

	public void setCCollection(
		java.util.Set<C> parameter) {
		this.cCollection = parameter;
	}
}
