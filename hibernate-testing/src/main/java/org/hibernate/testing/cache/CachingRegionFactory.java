/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.support.RegionFactoryTemplate;
import org.hibernate.cache.spi.support.StorageAccess;
import org.hibernate.cache.spi.support.DomainDataRegionImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 * @author Steve Ebersole
 */
public class CachingRegionFactory extends RegionFactoryTemplate {
	private static final Logger LOG = Logger.getLogger( CachingRegionFactory.class.getName() );

	public static String DEFAULT_ACCESSTYPE = "DefaultAccessType";
	private final CacheKeysFactory cacheKeysFactory;

	public CachingRegionFactory() {
		this( DefaultCacheKeysFactory.INSTANCE, null );
	}

	public CachingRegionFactory(CacheKeysFactory cacheKeysFactory) {
		this( cacheKeysFactory, null );
	}

	public CachingRegionFactory(Properties properties) {
		this( DefaultCacheKeysFactory.INSTANCE, properties );
	}

	public CachingRegionFactory(CacheKeysFactory cacheKeysFactory, Properties properties) {
		LOG.warn( "org.hibernate.testing.cache.CachingRegionFactory should be only used for testing." );
		this.cacheKeysFactory = cacheKeysFactory;
	}

	@Override
	protected void prepareForUse(SessionFactoryOptions settings, Map configValues) {
	}

	@Override
	public DomainDataRegion buildDomainDataRegion(
			DomainDataRegionConfig regionConfig, DomainDataRegionBuildingContext buildingContext) {
		return new DomainDataRegionImpl(
				regionConfig,
				this,
				new MapStorageAccessImpl(),
				cacheKeysFactory,
				buildingContext
		);
	}

	@Override
	protected StorageAccess createQueryResultsRegionStorageAccess(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		return new MapStorageAccessImpl();
	}

	@Override
	protected StorageAccess createTimestampsRegionStorageAccess(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		return new MapStorageAccessImpl();
	}

	@Override
	protected void releaseFromUse() {
	}
}
