/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.collection;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.infinispan.AdvancedCache;

/**
 * Collection region implementation
 *
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class CollectionRegionImpl extends BaseTransactionalDataRegion implements CollectionRegion {

   /**
    * Construct a collection region
    *
    * @param cache instance to store collection instances
    * @param name of collection type
    * @param metadata for the collection type
    * @param factory for the region
	* @param cacheKeysFactory factory for cache keys
    */
	public CollectionRegionImpl(
			AdvancedCache cache, String name,
			CacheDataDescription metadata, RegionFactory factory, CacheKeysFactory cacheKeysFactory) {
		super( cache, name, metadata, factory, cacheKeysFactory );
	}

	@Override
	public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		if ( AccessType.READ_ONLY.equals( accessType )
				|| AccessType.TRANSACTIONAL.equals( accessType ) ) {
			return new TransactionalAccess( this );
		}

		throw new CacheException( "Unsupported access type [" + accessType.getExternalName() + "]" );
	}

	public PutFromLoadValidator getPutFromLoadValidator() {
		return new PutFromLoadValidator( cache );
	}

}
