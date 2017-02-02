/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.entity;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.access.AccessDelegate;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;

import org.infinispan.AdvancedCache;

import javax.transaction.TransactionManager;

/**
 * Entity region implementation
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class EntityRegionImpl extends BaseTransactionalDataRegion implements EntityRegion {
	/**
	 * Construct a entity region
	 *  @param cache instance to store entity instances
	 * @param name of entity type
	 * @param transactionManager
	 * @param metadata for the entity type
	 * @param factory for the region
	 * @param cacheKeysFactory factory for cache keys
	 */
	public EntityRegionImpl(
			AdvancedCache cache, String name, TransactionManager transactionManager,
			CacheDataDescription metadata, InfinispanRegionFactory factory, CacheKeysFactory cacheKeysFactory) {
		super( cache, name, transactionManager, metadata, factory, cacheKeysFactory);
	}

	@Override
	public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		checkAccessType(accessType);
		AccessDelegate accessDelegate = createAccessDelegate(accessType);
		if ( accessType == AccessType.READ_ONLY || !getCacheDataDescription().isMutable() ) {
			return new ReadOnlyAccess( this, accessDelegate );
		}
		else {
			return new ReadWriteAccess( this, accessDelegate );
		}
	}
}
