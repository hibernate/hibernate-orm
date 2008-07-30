/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.cache;

import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.AccessType;

/**
 * Defines the contract for a cache region which will specifically be used to
 * store collection data.
 * <p/>
 * Impl note: Hibernate always deals with changes to collections which
 * (potentially) has its data in the L2 cache by removing that collection
 * data; in other words it never tries to update the cached state, thereby
 * allowing it to avoid a bunch of concurrency problems.
 *
 * @author Steve Ebersole
 */
public interface CollectionRegion extends TransactionalDataRegion {

	/**
	 * Build an access strategy for the requested access type.
	 *
	 * @param accessType The type of access strategy to build; never null.
	 * @return The appropriate strategy contract for accessing this region
	 * for the requested type of access.
	 * @throws CacheException Usually indicates mis-configuration.
	 */
	public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException;
}
