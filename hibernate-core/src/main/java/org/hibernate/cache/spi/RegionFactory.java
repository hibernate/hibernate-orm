/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
 * Contract for building second level cache regions.
 * <p/>
 * Implementors should define a constructor in one of two forms:<ul>
 *     <li>MyRegionFactoryImpl({@link java.util.Properties})</li>
 *     <li>MyRegionFactoryImpl()</li>
 * </ul>
 * Use the first when we need to read config properties prior to
 * {@link #start} being called.
 *
 * @author Steve Ebersole
 */
public interface RegionFactory extends Service, Stoppable {

	// These are names that users have to include in their caching configuration, do not change them
	String DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME = "default-query-results-region";
	String DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME = "default-update-timestamps-region";

	/**
	 * Lifecycle callback to perform any necessary initialization of the
	 * underlying cache provider.  Called exactly once during the
	 * construction of a {@link org.hibernate.internal.SessionFactoryImpl}.
	 *
	 * @param settings The settings in effect.
	 * @param configValues The available config values
	 *
	 * @throws org.hibernate.cache.CacheException Indicates problems starting the L2 cache impl;
	 * considered as a sign to stop {@link org.hibernate.SessionFactory}
	 * building.
	 */
	void start(SessionFactoryOptions settings, Map configValues) throws CacheException;

	/**
	 * By default should we perform "minimal puts" when using this second
	 * level cache implementation?
	 *
	 * @return True if "minimal puts" should be performed by default; false
	 *         otherwise.
	 */
	boolean isMinimalPutsEnabledByDefault();

	/**
	 * Get the default access type for any "user model" data
	 */
	AccessType getDefaultAccessType();

	String qualify(String regionName);

	default CacheTransactionSynchronization createTransactionContext(SharedSessionContractImplementor session) {
		return new StandardCacheTransactionSynchronization( this );
	}

	/**
	 * Generate a timestamp.  This value is generally used for purpose of
	 * locking/unlocking cache content depending upon the access-strategy being
	 * used.  The intended consumer of this method is the Session to manage
	 * its {@link SharedSessionContractImplementor#getTransactionStartTimestamp} value.
	 *
	 * It is also expected that this be the value used for this's RegionFactory's
	 * CacheTransactionContext
	 *
	 * @apiNote This "timestamp" need not be related to timestamp in the Java Date/millisecond
	 * sense.  It just needs to be an incrementing value
	 */
	long nextTimestamp();

	default long getTimeout() {
		// most existing providers defined this as 60 seconds.
		return 60000;
	}

	/**
	 * Create a named Region for holding domain model data
	 *
	 * @param regionConfig The user requested caching configuration for this Region
	 * @param buildingContext Access to delegates useful in building the Region
	 */
	DomainDataRegion buildDomainDataRegion(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext);


	QueryResultsRegion buildQueryResultsRegion(String regionName, SessionFactoryImplementor sessionFactory);

	TimestampsRegion buildTimestampsRegion(String regionName, SessionFactoryImplementor sessionFactory);
}
