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
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;

import org.jboss.logging.Logger;

/**
 * @author Eric Dalquist
 */
class NaturalIdRegionImpl extends BaseTransactionalDataRegion implements NaturalIdRegion {
	private static final Logger LOG = Logger.getLogger( NaturalIdRegionImpl.class.getName() );

	private final SessionFactoryOptions settings;

	NaturalIdRegionImpl(String name, CacheDataDescription metadata, SessionFactoryOptions settings) {
		super( name, metadata );
		this.settings = settings;
	}

	public SessionFactoryOptions getSettings() {
		return settings;
	}

	@Override
	public NaturalIdRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		switch ( accessType ) {
			case READ_ONLY:
				if ( getCacheDataDescription().isMutable() ) {
					LOG.warnf( "read-only cache configured for mutable collection [ %s ]", getName() );
				}
				return new ReadOnlyNaturalIdRegionAccessStrategy( this );
			case READ_WRITE:
				return new ReadWriteNaturalIdRegionAccessStrategy( this );
			case NONSTRICT_READ_WRITE:
				return new NonstrictReadWriteNaturalIdRegionAccessStrategy( this );
			case TRANSACTIONAL:
				return new TransactionalNaturalIdRegionAccessStrategy( this );
//				throw new UnsupportedOperationException( "doesn't support this access strategy" );
			default:
				throw new IllegalArgumentException( "unrecognized access strategy type [" + accessType + "]" );
		}
	}


}
