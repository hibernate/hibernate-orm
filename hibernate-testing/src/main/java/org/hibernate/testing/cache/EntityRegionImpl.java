/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 */
public class EntityRegionImpl extends BaseTransactionalDataRegion implements EntityRegion {
	private static final Logger LOG = Logger.getLogger( EntityRegionImpl.class );

	private final SessionFactoryOptions settings;

	protected EntityRegionImpl(
			CachingRegionFactory cachingRegionFactory,
			String name,
			CacheDataDescription metadata,
			SessionFactoryOptions settings) {
		super( cachingRegionFactory, name, metadata );
		this.settings = settings;

	}

	public SessionFactoryOptions getSettings() {
		return settings;
	}

	@Override
	public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		switch ( accessType ) {
			case READ_ONLY:
				if ( getCacheDataDescription().isMutable() ) {
					LOG.warnf( "read-only cache configured for mutable entity [ %s ]", getName() );
				}
				return new ReadOnlyEntityRegionAccessStrategy( this );
			case READ_WRITE:
				return new ReadWriteEntityRegionAccessStrategy( this );
			case NONSTRICT_READ_WRITE:
				return new NonstrictReadWriteEntityRegionAccessStrategy( this );
			case TRANSACTIONAL:
//				throw new UnsupportedOperationException( "doesn't support this access strategy" );
				return new TransactionalEntityRegionAccessStrategy( this );

			default:
				throw new IllegalArgumentException( "unrecognized access strategy type [" + accessType + "]" );
		}

	}

}
