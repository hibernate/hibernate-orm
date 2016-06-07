/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.cache;

import java.util.Properties;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 */
public class CachingRegionFactory implements RegionFactory {
	private static final Logger LOG = Logger.getLogger( CachingRegionFactory.class.getName() );

	public static String DEFAULT_ACCESSTYPE = "DefaultAccessType";

	private final CacheKeysFactory cacheKeysFactory;

	private SessionFactoryOptions settings;
	private Properties properties;

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
		LOG.warn( "CachingRegionFactory should be only used for testing." );
		this.cacheKeysFactory = cacheKeysFactory;
		this.properties = properties;
	}

	public CacheKeysFactory getCacheKeysFactory() {
		return cacheKeysFactory;
	}

	@Override
	public void start(SessionFactoryOptions settings, Properties properties) throws CacheException {
		this.settings = settings;
		this.properties = properties;
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
	public long nextTimestamp() {
		return Timestamper.next();
	}

	@Override
	public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		return new EntityRegionImpl( this, regionName, metadata, settings );
	}

	@Override
	public NaturalIdRegion buildNaturalIdRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		return new NaturalIdRegionImpl( this, regionName, metadata, settings );
	}

	@Override
	public CollectionRegion buildCollectionRegion(
			String regionName,
			Properties properties,
			CacheDataDescription metadata) throws CacheException {
		return new CollectionRegionImpl( this, regionName, metadata, settings );
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
		return new QueryResultsRegionImpl( this, regionName );
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
		return new TimestampsRegionImpl( this, regionName );
	}

	private static class QueryResultsRegionImpl extends BaseGeneralDataRegion implements QueryResultsRegion {
		QueryResultsRegionImpl(CachingRegionFactory cachingRegionFactory, String name) {
			super( cachingRegionFactory, name );
		}
	}

	private static class TimestampsRegionImpl extends BaseGeneralDataRegion implements TimestampsRegion {
		TimestampsRegionImpl(CachingRegionFactory cachingRegionFactory, String name) {
			super( cachingRegionFactory, name );
		}
	}
}
