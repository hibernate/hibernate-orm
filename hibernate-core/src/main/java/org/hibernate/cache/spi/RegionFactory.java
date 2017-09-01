/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Startable;
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
public interface RegionFactory extends Service, Startable, Stoppable {
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

	CacheTransactionContext startingTransaction(SharedSessionContractImplementor session);

	/**
	 * Generate a timestamp.  This value is generally used for purpose of
	 * locking/unlocking cache content depending upon the access-strategy being
	 * used.  The intended consumer of this method is the Session to manage
	 * its {@link SharedSessionContractImplementor#getTransactionStartTimestamp} value.
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
