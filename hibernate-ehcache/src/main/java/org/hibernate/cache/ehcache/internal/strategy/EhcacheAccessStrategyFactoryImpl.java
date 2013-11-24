/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.ehcache.internal.strategy;

import org.hibernate.cache.ehcache.EhCacheMessageLogger;
import org.hibernate.cache.ehcache.internal.regions.EhcacheCollectionRegion;
import org.hibernate.cache.ehcache.internal.regions.EhcacheEntityRegion;
import org.hibernate.cache.ehcache.internal.regions.EhcacheNaturalIdRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;

import org.jboss.logging.Logger;

/**
 * Class implementing {@link EhcacheAccessStrategyFactory}
 *
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public class EhcacheAccessStrategyFactoryImpl implements EhcacheAccessStrategyFactory {

	private static final EhCacheMessageLogger LOG = Logger.getMessageLogger(
			EhCacheMessageLogger.class,
			EhcacheAccessStrategyFactoryImpl.class.getName()
	);

	@Override
	public EntityRegionAccessStrategy createEntityRegionAccessStrategy(
			EhcacheEntityRegion entityRegion,
			AccessType accessType) {
		switch ( accessType ) {
			case READ_ONLY:
				if ( entityRegion.getCacheDataDescription().isMutable() ) {
					LOG.readOnlyCacheConfiguredForMutableEntity( entityRegion.getName() );
				}
				return new ReadOnlyEhcacheEntityRegionAccessStrategy( entityRegion, entityRegion.getSettings() );
			case READ_WRITE:
				return new ReadWriteEhcacheEntityRegionAccessStrategy( entityRegion, entityRegion.getSettings() );

			case NONSTRICT_READ_WRITE:
				return new NonStrictReadWriteEhcacheEntityRegionAccessStrategy(
						entityRegion,
						entityRegion.getSettings()
				);

			case TRANSACTIONAL:
				return new TransactionalEhcacheEntityRegionAccessStrategy(
						entityRegion,
						entityRegion.getEhcache(),
						entityRegion.getSettings()
				);
			default:
				throw new IllegalArgumentException( "unrecognized access strategy type [" + accessType + "]" );

		}

	}

	@Override
	public CollectionRegionAccessStrategy createCollectionRegionAccessStrategy(
			EhcacheCollectionRegion collectionRegion,
			AccessType accessType) {
		switch ( accessType ) {
			case READ_ONLY:
				if ( collectionRegion.getCacheDataDescription().isMutable() ) {
					LOG.readOnlyCacheConfiguredForMutableEntity( collectionRegion.getName() );
				}
				return new ReadOnlyEhcacheCollectionRegionAccessStrategy(
						collectionRegion,
						collectionRegion.getSettings()
				);
			case READ_WRITE:
				return new ReadWriteEhcacheCollectionRegionAccessStrategy(
						collectionRegion,
						collectionRegion.getSettings()
				);
			case NONSTRICT_READ_WRITE:
				return new NonStrictReadWriteEhcacheCollectionRegionAccessStrategy(
						collectionRegion,
						collectionRegion.getSettings()
				);
			case TRANSACTIONAL:
				return new TransactionalEhcacheCollectionRegionAccessStrategy(
						collectionRegion, collectionRegion.getEhcache(), collectionRegion
						.getSettings()
				);
			default:
				throw new IllegalArgumentException( "unrecognized access strategy type [" + accessType + "]" );
		}
	}

	@Override
	public NaturalIdRegionAccessStrategy createNaturalIdRegionAccessStrategy(
			EhcacheNaturalIdRegion naturalIdRegion,
			AccessType accessType) {
		switch ( accessType ) {
			case READ_ONLY:
				if ( naturalIdRegion.getCacheDataDescription().isMutable() ) {
					LOG.readOnlyCacheConfiguredForMutableEntity( naturalIdRegion.getName() );
				}
				return new ReadOnlyEhcacheNaturalIdRegionAccessStrategy(
						naturalIdRegion,
						naturalIdRegion.getSettings()
				);
			case READ_WRITE:
				return new ReadWriteEhcacheNaturalIdRegionAccessStrategy(
						naturalIdRegion,
						naturalIdRegion.getSettings()
				);
			case NONSTRICT_READ_WRITE:
				return new NonStrictReadWriteEhcacheNaturalIdRegionAccessStrategy(
						naturalIdRegion,
						naturalIdRegion.getSettings()
				);
			case TRANSACTIONAL:
				return new TransactionalEhcacheNaturalIdRegionAccessStrategy(
						naturalIdRegion, naturalIdRegion.getEhcache(), naturalIdRegion
						.getSettings()
				);
			default:
				throw new IllegalArgumentException( "unrecognized access strategy type [" + accessType + "]" );
		}
	}


}
