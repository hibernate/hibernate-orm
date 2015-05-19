/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;

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
	 * @throws org.hibernate.cache.CacheException Usually indicates mis-configuration.
	 */
	public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException;
}
