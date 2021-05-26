/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.spi;

import org.hibernate.loader.plan.spi.CollectionQuerySpace;
import org.hibernate.loader.plan.spi.Join;

/**
 * Describes a collection query space that allows adding joins with other
 * query spaces; used while building a {@link CollectionQuerySpace}.
 *
 * @see org.hibernate.loader.plan.spi.Join
 *
 * @author Gail Badner
 */
public interface ExpandingCollectionQuerySpace extends CollectionQuerySpace, ExpandingQuerySpace {

	/**
	 * Adds a join with another query space for either a collection element or index.
	 *
	 * If {@code join} is an instance of {@link org.hibernate.loader.plan.spi.JoinDefinedByMetadata}, then the only valid
	 * values returned by {@link org.hibernate.loader.plan.spi.JoinDefinedByMetadata#getJoinedPropertyName}
	 * are {@code "elements"} and {@code "indices"} for the collection element or index, respectively.
	 *
	 * @param join The element or index join to add.
	 *
	 * @throws java.lang.IllegalArgumentException if {@code join} is an instance of
	 * {@link org.hibernate.loader.plan.spi.JoinDefinedByMetadata} and {@code join.getJoinedPropertyName()}
	 * is neither {@code "elements"} and {@code "indices"}.
	 * @throws java.lang.IllegalStateException if there is already an existing join with the same joined property name.
	 */
	void addJoin(Join join);
}
