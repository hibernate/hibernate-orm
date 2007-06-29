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
