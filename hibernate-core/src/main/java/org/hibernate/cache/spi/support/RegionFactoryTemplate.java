/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi.support;

import jakarta.annotation.Nonnull;
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
	@Nonnull
	public DomainDataRegion buildDomainDataRegion(
			@Nonnull DomainDataRegionConfig regionConfig,
			@Nonnull DomainDataRegionBuildingContext buildingContext) {
		verifyStarted();
		return new DomainDataRegionTemplate(
				regionConfig,
				this,
				createDomainDataStorageAccess( regionConfig, buildingContext ),
				getImplicitCacheKeysFactory(),
				buildingContext
		);
	}

	@Nonnull
	protected CacheKeysFactory getImplicitCacheKeysFactory() {
		return DefaultCacheKeysFactory.INSTANCE;
	}

	@Nonnull
	protected DomainDataStorageAccess createDomainDataStorageAccess(
			@Nonnull DomainDataRegionConfig regionConfig,
			@Nonnull DomainDataRegionBuildingContext buildingContext) {
		throw new UnsupportedOperationException( "Not implemented by caching provider" );
	}

	@Override
	@Nonnull
	public QueryResultsRegion buildQueryResultsRegion(
			@Nonnull String regionName,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		verifyStarted();
		return new QueryResultsRegionTemplate(
				regionName,
				this,
				createQueryResultsRegionStorageAccess( regionName, sessionFactory )
		);
	}

	@Nonnull
	protected abstract StorageAccess createQueryResultsRegionStorageAccess(
			@Nonnull String regionName,
			@Nonnull SessionFactoryImplementor sessionFactory);

	@Override
	@Nonnull
	public TimestampsRegion buildTimestampsRegion(
			@Nonnull String regionName,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		verifyStarted();
		return new TimestampsRegionTemplate(
				regionName,
				this,
				createTimestampsRegionStorageAccess( regionName, sessionFactory )
		);
	}

	@Nonnull
	protected abstract StorageAccess createTimestampsRegionStorageAccess(
			@Nonnull String regionName,
			@Nonnull SessionFactoryImplementor sessionFactory);
}
