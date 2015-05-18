/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
	 * {@link org.hibernate.loader.plan.spi.JoinDefinedByMetadata} and {@code join.getJoinedPropertyName()
	 * is neither {@code "elements"} and {@code "indices"}.
	 * @throws java.lang.IllegalStateException if there is already an existing join with the same joined property name.
	 */
	void addJoin(Join join);
}
