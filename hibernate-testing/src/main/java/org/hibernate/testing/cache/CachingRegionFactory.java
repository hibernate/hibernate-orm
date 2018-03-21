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
import org.hibernate.cache.CacheException;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.DomainDataRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.support.RegionNameQualifier;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 * @author Steve Ebersole
 */
public class CachingRegionFactory implements RegionFactory {
	private static final Logger LOG = Logger.getLogger( CachingRegionFactory.class.getName() );

	public static String DEFAULT_ACCESSTYPE = "DefaultAccessType";

	private final CacheKeysFactory cacheKeysFactory;

	private Properties properties;
	private SessionFactoryOptions options;

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
		this.properties = properties;
	}

	public CacheKeysFactory getCacheKeysFactory() {
		return cacheKeysFactory;
	}

	@Override
	public void start(SessionFactoryOptions settings, Map configValues) throws CacheException {
		options = settings;
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
		if ( properties != null && properties.get( DEFAULT_ACCESSTYPE ) != null ) {
			return AccessType.fromExternalName( properties.getProperty( DEFAULT_ACCESSTYPE ) );
		}
		return AccessType.READ_WRITE;
	}

	@Override
	public String qualify(String regionName) {
		return RegionNameQualifier.INSTANCE.qualify( regionName, options );
	}

	@Override
	public long nextTimestamp() {
//		return System.currentTimeMillis();
		return Timestamper.next();
	}

	@Override
	public long getTimeout() {
		return Timestamper.ONE_MS * 60000;
	}

	@Override
	public DomainDataRegion buildDomainDataRegion(
			DomainDataRegionConfig regionConfig,
			DomainDataRegionBuildingContext buildingContext) {
		return new DomainDataRegionImpl( regionConfig, this, cacheKeysFactory, buildingContext );
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		return new QueryResultsRegionImpl( regionName, this );
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(
			String regionName,
			SessionFactoryImplementor sessionFactory) {
		return new TimestampsRegionImpl( regionName, this );
	}

}
