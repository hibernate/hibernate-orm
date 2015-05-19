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
import org.hibernate.cache.spi.CacheDataDescription;
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
	private SessionFactoryOptions settings;
	private Properties properties;

	public CachingRegionFactory() {
		LOG.warn( "CachingRegionFactory should be only used for testing." );
	}

	public CachingRegionFactory(Properties properties) {
		//add here to avoid run into catch
		LOG.warn( "CachingRegionFactory should be only used for testing." );
		this.properties = properties;
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
		return new EntityRegionImpl( regionName, metadata, settings );
	}

	@Override
	public NaturalIdRegion buildNaturalIdRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		return new NaturalIdRegionImpl( regionName, metadata, settings );
	}

	@Override
	public CollectionRegion buildCollectionRegion(
			String regionName,
			Properties properties,
			CacheDataDescription metadata)
			throws CacheException {
		return new CollectionRegionImpl( regionName, metadata, settings );
	}

	@Override
	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
		return new QueryResultsRegionImpl( regionName );
	}

	@Override
	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
		return new TimestampsRegionImpl( regionName );
	}

	private static class QueryResultsRegionImpl extends BaseGeneralDataRegion implements QueryResultsRegion {
		QueryResultsRegionImpl(String name) {
			super( name );
		}
	}

	private static class TimestampsRegionImpl extends BaseGeneralDataRegion implements TimestampsRegion {
		TimestampsRegionImpl(String name) {
			super( name );
		}
	}
}
