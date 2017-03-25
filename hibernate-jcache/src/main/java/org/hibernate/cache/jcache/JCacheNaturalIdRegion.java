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
import org.hibernate.cache.jcache.access.NonStrictNaturalIdRegionAccessStrategy;
import org.hibernate.cache.jcache.access.ReadOnlyNaturalIdRegionAccessStrategy;
import org.hibernate.cache.jcache.access.ReadWriteNaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;

/**
 * @author Alex Snaps
 */
public class JCacheNaturalIdRegion extends JCacheTransactionalDataRegion implements NaturalIdRegion {

	public JCacheNaturalIdRegion(Cache<Object, Object> cache, CacheDataDescription metadata, SessionFactoryOptions options) {
		super( cache, metadata, options );
	}

	@Override
	public NaturalIdRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		switch ( accessType ) {
			case READ_ONLY:
				return new ReadOnlyNaturalIdRegionAccessStrategy( this );
			case NONSTRICT_READ_WRITE:
				return new NonStrictNaturalIdRegionAccessStrategy( this );
			case READ_WRITE:
				return new ReadWriteNaturalIdRegionAccessStrategy( this );
			case TRANSACTIONAL:
				throw new UnsupportedOperationException( "Implement me!" );
			default:
				throw new UnsupportedOperationException( "Unknown AccessType: " + accessType.name() );
		}
	}
}
