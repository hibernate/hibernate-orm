/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 */
class CollectionRegionImpl extends BaseTransactionalDataRegion implements CollectionRegion {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class, CollectionRegionImpl.class.getName()
	);
	private final Settings settings;
	CollectionRegionImpl(String name, CacheDataDescription metadata, Settings settings) {
		super( name, metadata );
		this.settings=settings;
	}

	public Settings getSettings() {
		return settings;
	}

	@Override
	public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		switch ( accessType ) {
			case READ_ONLY:
				if ( getCacheDataDescription().isMutable() ) {
					LOG.warnf( "read-only cache configured for mutable collection [ %s ]", getName() );
				}
				return new ReadOnlyCollectionRegionAccessStrategy( this );
			case READ_WRITE:
				 return new ReadWriteCollectionRegionAccessStrategy( this );
			case NONSTRICT_READ_WRITE:
				return new NonstrictReadWriteCollectionRegionAccessStrategy( this );
			case TRANSACTIONAL:
				return new TransactionalCollectionRegionAccessStrategy( this );
//				throw new UnsupportedOperationException( "doesn't support this access strategy" );
			default:
				throw new IllegalArgumentException( "unrecognized access strategy type [" + accessType + "]" );
		}
	}


}
