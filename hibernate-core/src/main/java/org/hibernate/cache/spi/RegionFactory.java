/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import java.util.Map;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Stoppable;

/**
 * Contract for building second-level cache regions, including
 * regions dedicated to storing:
 * <ul>
 * <li>{@linkplain #buildDomainDataRegion entity and collection}
 *     instances,
 * <li>{@linkplain #buildQueryResultsRegion query result sets},
 *     and
 * <li>{@linkplain #buildTimestampsRegion timestamps} used to
 *     determine when a cached query result set is stale.
 * </ul>
 * <p>
 * Implementors should define a constructor in one of two forms:
 * <ul>
 * <li>{@code MyRegionFactoryImpl(java.util.Properties)}</li>
 * <li>{@code MyRegionFactoryImpl()}</li>
 * </ul>
 * <p>
 * Use the first when we need to read config properties prior to
 * {@link #start} being called.
 * <p>
 * A {@code RegionFactory} may be selected using the property
 * {@value org.hibernate.cfg.AvailableSettings#CACHE_REGION_FACTORY}.
 *
 * @author Steve Ebersole
 */
public interface RegionFactory extends Service, Stoppable {

	// These are names that users have to include in their caching configuration, do not change them
	String DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME = "default-query-results-region";
	String DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME = "default-update-timestamps-region";

	/**
	 * Lifecycle callback to perform any necessary initialization of
	 * the underlying cache provider. Called exactly once during the
	 * construction of a {@link org.hibernate.internal.SessionFactoryImpl}.
	 *
	 * @param settings The settings in effect.
	 * @param configValues The available config values
	 *
	 * @throws CacheException Indicates problems starting the L2 cache impl;
	 * considered as a sign to stop {@link org.hibernate.SessionFactory}
	 * building.
	 */
	void start(SessionFactoryOptions settings, Map<String,Object> configValues) throws CacheException;

	/**
	 * By default, should we perform "minimal puts" when using this second
	 * level cache implementation?
	 *
	 * @return True if "minimal puts" should be performed by default; false
	 *         otherwise.
	 */
	boolean isMinimalPutsEnabledByDefault();

	/**
	 * Get the default access type for any "user model" data.
	 */
	AccessType getDefaultAccessType();

	String qualify(String regionName);

	default CacheTransactionSynchronization createTransactionContext(SharedSessionContractImplementor session) {
		return new StandardCacheTransactionSynchronization( this );
	}

	/**
	 * Generate a timestamp. This value is generally used for purpose of
	 * locking/unlocking cache content depending upon the access strategy
	 * being used. It's also expected that this be the value used by the
	 * {@link #createTransactionContext CacheTransactionSynchronization}
	 * created by this {@code RegionFactory}.
	 *
	 * @apiNote This "timestamp" need not be related to timestamp in the
	 * {@link java.util.Date#getTime()}/{@link System#currentTimeMillis()}
	 * sense.  It just needs to be an incrementing value.
	 */
	long nextTimestamp();

	default long getTimeout() {
		// most existing providers defined this as 60 seconds.
		return 60000;
	}

	/**
	 * Create a named {@link Region} for holding domain model data
	 *
	 * @param regionConfig The user requested caching configuration for this Region
	 * @param buildingContext Access to delegates useful in building the Region
	 */
	DomainDataRegion buildDomainDataRegion(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext);


	/**
	 * Create a named {@link Region} for holding query result sets.
	 */
	QueryResultsRegion buildQueryResultsRegion(String regionName, SessionFactoryImplementor sessionFactory);

	/**
	 * Create a named {@link Region} for holding timestamps used to
	 * determine when a cached query result set is stale.
	 */
	TimestampsRegion buildTimestampsRegion(String regionName, SessionFactoryImplementor sessionFactory);
}
