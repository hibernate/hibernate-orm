/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing.cache;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Settings;
import org.hibernate.internal.CoreMessageLogger;

import org.jboss.logging.Logger;

/**
 * @author Strong Liu
 */
public class CachingRegionFactory implements RegionFactory {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class, CachingRegionFactory.class.getName()
	);
	public static String DEFAULT_ACCESSTYPE = "DefaultAccessType";
	private Settings settings;
	private Properties properties;
	public CachingRegionFactory() {
		LOG.warn( "CachingRegionFactory should be only used for testing." );
	}

	public CachingRegionFactory(Properties properties) {
		//add here to avoid run into catch
		LOG.warn( "CachingRegionFactory should be only used for testing." );
		this.properties=properties; 
	}

	@Override
	public void start(Settings settings, Properties properties) throws CacheException {
		this.settings=settings;
		this.properties=properties; 
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
		if (properties != null && properties.get(DEFAULT_ACCESSTYPE) != null) {
			return AccessType.fromExternalName(properties.getProperty(DEFAULT_ACCESSTYPE));
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
	public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata)
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
