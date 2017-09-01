/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import org.hibernate.cache.NoCacheRegionFactoryAvailableException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.CacheTransactionContext;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Factory used if no caching enabled in config...
 *
 * @author Steve Ebersole
 */
public class NoCachingRegionFactory implements RegionFactory {
	/**
	 * Singleton access
	 */
	public static final NoCachingRegionFactory INSTANCE = new NoCachingRegionFactory();

	/**
	 * Constructs a NoCachingRegionFactory.  Although access should generally use {@link #INSTANCE}
	 */
	public NoCachingRegionFactory() {
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public boolean isMinimalPutsEnabledByDefault() {
		return false;
	}

	@Override
	public AccessType getDefaultAccessType() {
		return null;
	}

	@Override
	public CacheTransactionContext startingTransaction(SharedSessionContractImplementor session) {
		return new CacheTransactionContext() {
			final long timestamp = System.currentTimeMillis();

			@Override
			public long getCurrentTransactionStartTimestamp() {
				return timestamp;
			}
		};
	}

	@Override
	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	@Override
	public DomainDataRegion buildDomainDataRegion(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext) {
		throw new NoCacheRegionFactoryAvailableException();
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		throw new NoCacheRegionFactoryAvailableException();
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		throw new NoCacheRegionFactoryAvailableException();
	}
}
