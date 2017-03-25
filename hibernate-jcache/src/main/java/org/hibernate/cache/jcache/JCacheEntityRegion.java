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
import org.hibernate.cache.jcache.access.NonStrictEntityRegionAccessStrategy;
import org.hibernate.cache.jcache.access.ReadOnlyEntityRegionAccessStrategy;
import org.hibernate.cache.jcache.access.ReadWriteEntityRegionAccessStrategy;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;

/**
 * @author Alex Snaps
 */
public class JCacheEntityRegion extends JCacheTransactionalDataRegion implements EntityRegion {

	public JCacheEntityRegion(Cache<Object, Object> cache, CacheDataDescription metadata, SessionFactoryOptions options) {
		super( cache, metadata, options );
	}

	@Override
	public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		throwIfAccessTypeUnsupported( accessType );
		switch ( accessType ) {
			case READ_ONLY:
				return new ReadOnlyEntityRegionAccessStrategy( this );
			case NONSTRICT_READ_WRITE:
				return new NonStrictEntityRegionAccessStrategy( this );
			case READ_WRITE:
				return new ReadWriteEntityRegionAccessStrategy( this );
			case TRANSACTIONAL:
				return createTransactionalEntityRegionAccessStrategy();
			default:
				throw new IllegalArgumentException( "Unknown AccessType: " + accessType );
		}
	}

	protected EntityRegionAccessStrategy createTransactionalEntityRegionAccessStrategy() {
		throw new UnsupportedOperationException("No org.hibernate.cache.spi.access.AccessType.TRANSACTIONAL support");
	}
}
