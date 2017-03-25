/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache;

import javax.cache.Cache;
import org.hibernate.boot.spi.SessionFactoryOptions;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.jcache.access.NonStrictCollectionRegionAccessStrategy;
import org.hibernate.cache.jcache.access.ReadOnlyCollectionRegionAccessStrategy;
import org.hibernate.cache.jcache.access.ReadWriteCollectionRegionAccessStrategy;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;

/**
 * @author Alex Snaps
 */
public class JCacheCollectionRegion extends JCacheTransactionalDataRegion implements CollectionRegion {

	public JCacheCollectionRegion(Cache<Object, Object> cache, CacheDataDescription metadata, SessionFactoryOptions options) {
		super( cache, metadata, options );
	}

	@Override
	public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		switch ( accessType ) {
			case READ_ONLY:
				return new ReadOnlyCollectionRegionAccessStrategy( this );
			case NONSTRICT_READ_WRITE:
				return new NonStrictCollectionRegionAccessStrategy( this );
			case READ_WRITE:
				return new ReadWriteCollectionRegionAccessStrategy( this );
			case TRANSACTIONAL:
				throw new UnsupportedOperationException( "Implement me!" );
			default:
				throw new UnsupportedOperationException( "Unknown AccessType: " + accessType.name() );
		}
	}
}
