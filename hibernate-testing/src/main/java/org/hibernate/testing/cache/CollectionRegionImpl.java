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
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 */
class CollectionRegionImpl extends BaseTransactionalDataRegion implements CollectionRegion {
	private static final Logger LOG = Logger.getLogger( CollectionRegionImpl.class.getName() );

	private final SessionFactoryOptions settings;

	CollectionRegionImpl(String name, CacheDataDescription metadata, SessionFactoryOptions settings) {
		super( name, metadata );
		this.settings = settings;
	}

	public SessionFactoryOptions getSettings() {
		return settings;
	}

	@Override
	public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		switch ( accessType ) {
			case READ_ONLY: {
				if ( getCacheDataDescription().isMutable() ) {
					LOG.warnf( "read-only cache configured for mutable collection [ %s ]", getName() );
				}
				return new ReadOnlyCollectionRegionAccessStrategy( this );
			}
			case READ_WRITE: {
				return new ReadWriteCollectionRegionAccessStrategy( this );
			}
			case NONSTRICT_READ_WRITE: {
				return new NonstrictReadWriteCollectionRegionAccessStrategy( this );
			}
			case TRANSACTIONAL: {
				return new TransactionalCollectionRegionAccessStrategy( this );
//				throw new UnsupportedOperationException( "doesn't support this access strategy" );
			}
			default: {
				throw new IllegalArgumentException( "unrecognized access strategy type [" + accessType + "]" );
			}
		}
	}

}
