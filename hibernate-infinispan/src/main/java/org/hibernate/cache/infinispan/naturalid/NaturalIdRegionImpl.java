/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.naturalid;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.access.AccessDelegate;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.infinispan.AdvancedCache;

import javax.transaction.TransactionManager;

/**
 * Natural ID cache region
 *
 * @author Strong Liu <stliu@hibernate.org>
 * @author Galder Zamarreño
 */
public class NaturalIdRegionImpl extends BaseTransactionalDataRegion
		implements NaturalIdRegion {

	/**
	 * Constructor for the natural id region.
	 *  @param cache instance to store natural ids
	 * @param name of natural id region
	 * @param transactionManager
	 * @param metadata for the natural id region
	 * @param factory for the natural id region
	 * @param cacheKeysFactory factory for cache keys
	 */
	public NaturalIdRegionImpl(
			AdvancedCache cache, String name, TransactionManager transactionManager,
			CacheDataDescription metadata, InfinispanRegionFactory factory, CacheKeysFactory cacheKeysFactory) {
		super( cache, name, transactionManager, metadata, factory, cacheKeysFactory );
	}

	@Override
	public NaturalIdRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		checkAccessType( accessType );
		AccessDelegate accessDelegate = createAccessDelegate(accessType);
		if ( accessType == AccessType.READ_ONLY || !getCacheDataDescription().isMutable() ) {
			return new ReadOnlyAccess( this, accessDelegate );
		}
		else {
			return new ReadWriteAccess( this, accessDelegate );
		}
	}
}
