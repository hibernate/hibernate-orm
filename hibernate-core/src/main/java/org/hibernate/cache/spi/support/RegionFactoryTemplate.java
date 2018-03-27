/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.AbstractRegionFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class RegionFactoryTemplate extends AbstractRegionFactory {
	@Override
	public DomainDataRegion buildDomainDataRegion(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext) {
		verifyStarted();
		return new DomainDataRegionTemplate(
				regionConfig,
				this,
				createDomainDataStorageAccess( regionConfig, buildingContext ),
				getImplicitCacheKeysFactory(),
				buildingContext
		);
	}

	protected CacheKeysFactory getImplicitCacheKeysFactory() {
		return DefaultCacheKeysFactory.INSTANCE;
	}

	protected DomainDataStorageAccess createDomainDataStorageAccess(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext) {
		throw new UnsupportedOperationException( "Not implemented by caching provider" );
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		verifyStarted();
		return new QueryResultsRegionTemplate(
				regionName,
				this,
				createQueryResultsRegionStorageAccess( regionName, sessionFactory )
		);
	}

	protected abstract StorageAccess createQueryResultsRegionStorageAccess(
			String regionName,
			SessionFactoryImplementor sessionFactory);

	@Override
	public TimestampsRegion buildTimestampsRegion(
			String regionName, SessionFactoryImplementor sessionFactory) {
		verifyStarted();
		return new TimestampsRegionTemplate(
				regionName,
				this,
				createTimestampsRegionStorageAccess( regionName, sessionFactory )
		);
	}

	protected abstract StorageAccess createTimestampsRegionStorageAccess(
			String regionName,
			SessionFactoryImplementor sessionFactory);
}
