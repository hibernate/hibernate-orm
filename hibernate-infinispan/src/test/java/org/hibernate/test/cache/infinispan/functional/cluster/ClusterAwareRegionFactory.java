/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat, Inc. and/or it's affiliates, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.hibernate.test.cache.infinispan.functional.cluster;

import java.util.Hashtable;
import java.util.Properties;

import org.hibernate.test.cache.infinispan.functional.SingleNodeTestCase;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.spi.AbstractRegionFactory;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.service.spi.InjectService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * ClusterAwareRegionFactory.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class ClusterAwareRegionFactory extends AbstractRegionFactory {

	private static final Log log = LogFactory.getLog( ClusterAwareRegionFactory.class );
	private static final Hashtable<String, EmbeddedCacheManager> cacheManagers = new Hashtable<String, EmbeddedCacheManager>();

	private final InfinispanRegionFactory delegate =
			new SingleNodeTestCase.TestInfinispanRegionFactory();
	private String cacheManagerName;
	private boolean locallyAdded;

	public static EmbeddedCacheManager getCacheManager(String name) {
		return cacheManagers.get( name );
	}

	public static void addCacheManager(String name, EmbeddedCacheManager manager) {
		cacheManagers.put( name, manager );
	}

	public static void clearCacheManagers() {
		for ( EmbeddedCacheManager manager : cacheManagers.values() ) {
			try {
				manager.stop();
			}
			catch ( Exception e ) {
				log.error( "Exception cleaning up CacheManager " + manager, e );
			}
		}
		cacheManagers.clear();
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		super.injectServices( serviceRegistry );
		delegate.injectServices( serviceRegistry );

	}

	@InjectService
	public void setConfigurationService(ConfigurationService configurationService) {
		cacheManagerName = configurationService.getSetting( DualNodeTestCase.NODE_ID_PROP, StandardConverters.STRING );
	}

	@Override
	public void start() {

		EmbeddedCacheManager existing = getCacheManager( cacheManagerName );
		locallyAdded = ( existing == null );

		if ( locallyAdded ) {
			delegate.start();
			cacheManagers.put( cacheManagerName, delegate.getCacheManager() );
		}
		else {
			delegate.initGenericDataTypeOverrides();
			delegate.setCacheManager( existing );
		}
	}

	public void start(Settings settings, Properties properties) throws CacheException {
		start();
	}

	public void stop() {
		if ( locallyAdded ) {
			cacheManagers.remove( cacheManagerName );
		}
		delegate.stop();
	}

	public CollectionRegion buildCollectionRegion(String regionName, Properties properties,
												  CacheDataDescription metadata) throws CacheException {
		return delegate.buildCollectionRegion( regionName, properties, metadata );
	}

	public EntityRegion buildEntityRegion(String regionName, Properties properties,
										  CacheDataDescription metadata) throws CacheException {
		return delegate.buildEntityRegion( regionName, properties, metadata );
	}

	@Override
	public NaturalIdRegion buildNaturalIdRegion(String regionName, Properties properties, CacheDataDescription metadata)
			throws CacheException {
		return delegate.buildNaturalIdRegion( regionName, properties, metadata );
	}

	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties)
			throws CacheException {
		return delegate.buildQueryResultsRegion( regionName, properties );
	}

	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties)
			throws CacheException {
		return delegate.buildTimestampsRegion( regionName, properties );
	}

	public boolean isMinimalPutsEnabledByDefault() {
		return delegate.isMinimalPutsEnabledByDefault();
	}

	@Override
	public AccessType getDefaultAccessType() {
		return AccessType.TRANSACTIONAL;
	}

	public long nextTimestamp() {
		return delegate.nextTimestamp();
	}
}
