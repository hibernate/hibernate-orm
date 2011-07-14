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
package org.hibernate.cache.ehcache.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cache.ehcache.regions.EhcacheCollectionRegion;
import org.hibernate.cache.ehcache.regions.EhcacheEntityRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;

/**
 * Class implementing {@link EhcacheAccessStrategyFactory}
 *
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
public class EhcacheAccessStrategyFactoryImpl implements EhcacheAccessStrategyFactory {

	private static final Logger LOG = LoggerFactory.getLogger( EhcacheAccessStrategyFactoryImpl.class );

	/**
	 * {@inheritDoc}
	 */
	public EntityRegionAccessStrategy createEntityRegionAccessStrategy(EhcacheEntityRegion entityRegion, AccessType accessType) {
		if ( AccessType.READ_ONLY.equals( accessType ) ) {
			if ( entityRegion.getCacheDataDescription().isMutable() ) {
				LOG.warn( "read-only cache configured for mutable entity [" + entityRegion.getName() + "]" );
			}
			return new ReadOnlyEhcacheEntityRegionAccessStrategy( entityRegion, entityRegion.getSettings() );
		}
		else if ( AccessType.READ_WRITE.equals( accessType ) ) {
			return new ReadWriteEhcacheEntityRegionAccessStrategy( entityRegion, entityRegion.getSettings() );
		}
		else if ( AccessType.NONSTRICT_READ_WRITE.equals( accessType ) ) {
			return new NonStrictReadWriteEhcacheEntityRegionAccessStrategy( entityRegion, entityRegion.getSettings() );
		}
		else if ( AccessType.TRANSACTIONAL.equals( accessType ) ) {
			return new TransactionalEhcacheEntityRegionAccessStrategy(
					entityRegion,
					entityRegion.getEhcache(),
					entityRegion.getSettings()
			);
		}
		else {
			throw new IllegalArgumentException( "unrecognized access strategy type [" + accessType + "]" );
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public CollectionRegionAccessStrategy createCollectionRegionAccessStrategy(EhcacheCollectionRegion collectionRegion,
																			   AccessType accessType) {
		if ( AccessType.READ_ONLY.equals( accessType ) ) {
			if ( collectionRegion.getCacheDataDescription().isMutable() ) {
				LOG.warn( "read-only cache configured for mutable entity [" + collectionRegion.getName() + "]" );
			}
			return new ReadOnlyEhcacheCollectionRegionAccessStrategy(
					collectionRegion,
					collectionRegion.getSettings()
			);
		}
		else if ( AccessType.READ_WRITE.equals( accessType ) ) {
			return new ReadWriteEhcacheCollectionRegionAccessStrategy(
					collectionRegion,
					collectionRegion.getSettings()
			);
		}
		else if ( AccessType.NONSTRICT_READ_WRITE.equals( accessType ) ) {
			return new NonStrictReadWriteEhcacheCollectionRegionAccessStrategy(
					collectionRegion,
					collectionRegion.getSettings()
			);
		}
		else if ( AccessType.TRANSACTIONAL.equals( accessType ) ) {
			return new TransactionalEhcacheCollectionRegionAccessStrategy(
					collectionRegion, collectionRegion.getEhcache(), collectionRegion
					.getSettings()
			);
		}
		else {
			throw new IllegalArgumentException( "unrecognized access strategy type [" + accessType + "]" );
		}
	}

}
