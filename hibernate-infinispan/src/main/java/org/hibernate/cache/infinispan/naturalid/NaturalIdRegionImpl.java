/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.naturalid;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.infinispan.AdvancedCache;

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
    *
    * @param cache instance to store natural ids
    * @param name of natural id region
    * @param metadata for the natural id region
    * @param factory for the natural id region
	* @param cacheKeysFactory factory for cache keys
    */
	public NaturalIdRegionImpl(
			AdvancedCache cache, String name,
			CacheDataDescription metadata, RegionFactory factory, CacheKeysFactory cacheKeysFactory) {
		super( cache, name, metadata, factory, cacheKeysFactory );
	}

	@Override
	public NaturalIdRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		switch ( accessType ) {
			case READ_ONLY:
				return new ReadOnlyAccess( this );
			case TRANSACTIONAL:
				return new TransactionalAccess( this );
			default:
				throw new CacheException( "Unsupported access type [" + accessType.getExternalName() + "]" );
		}
	}

	public PutFromLoadValidator getPutFromLoadValidator() {
		return new PutFromLoadValidator( cache );
	}

}
