/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.cache.infinispan;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.collection.CollectionRegionImpl;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.StandardServiceRegistryImpl;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.infinispan.context.Flag;

/**
 * Defines the environment for a node.
 *
 * @author Steve Ebersole
 */
public class NodeEnvironment {
	private final Configuration configuration;

	private StandardServiceRegistryImpl serviceRegistry;
	private InfinispanRegionFactory regionFactory;

	private Map<String,EntityRegionImpl> entityRegionMap;
	private Map<String,CollectionRegionImpl> collectionRegionMap;

	public NodeEnvironment(Configuration configuration) {
		this.configuration = configuration;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public StandardServiceRegistryImpl getServiceRegistry() {
		return serviceRegistry;
	}

	public EntityRegionImpl getEntityRegion(String name, CacheDataDescription cacheDataDescription) {
		if ( entityRegionMap == null ) {
			entityRegionMap = new HashMap<String, EntityRegionImpl>();
			return buildAndStoreEntityRegion( name, cacheDataDescription );
		}
		EntityRegionImpl region = entityRegionMap.get( name );
		if ( region == null ) {
			region = buildAndStoreEntityRegion( name, cacheDataDescription );
		}
		return region;
	}

	private EntityRegionImpl buildAndStoreEntityRegion(String name, CacheDataDescription cacheDataDescription) {
		EntityRegionImpl region = (EntityRegionImpl) regionFactory.buildEntityRegion(
				name,
				configuration.getProperties(),
				cacheDataDescription
		);
		entityRegionMap.put( name, region );
		return region;
	}

	public CollectionRegionImpl getCollectionRegion(String name, CacheDataDescription cacheDataDescription) {
		if ( collectionRegionMap == null ) {
			collectionRegionMap = new HashMap<String, CollectionRegionImpl>();
			return buildAndStoreCollectionRegion( name, cacheDataDescription );
		}
		CollectionRegionImpl region = collectionRegionMap.get( name );
		if ( region == null ) {
			region = buildAndStoreCollectionRegion( name, cacheDataDescription );
			collectionRegionMap.put( name, region );
		}
		return region;
	}

	private CollectionRegionImpl buildAndStoreCollectionRegion(String name, CacheDataDescription cacheDataDescription) {
		CollectionRegionImpl region;
		region = (CollectionRegionImpl) regionFactory.buildCollectionRegion(
				name,
				configuration.getProperties(),
				cacheDataDescription
		);
		return region;
	}

	public void prepare() throws Exception {
		serviceRegistry = (StandardServiceRegistryImpl) new ServiceRegistryBuilder()
				.applySettings( configuration.getProperties() )
				.buildServiceRegistry();
		regionFactory = CacheTestUtil.startRegionFactory( serviceRegistry, configuration );
	}

	public void release() throws Exception {
		if ( entityRegionMap != null ) {
			for ( final EntityRegionImpl region : entityRegionMap.values() ) {
				Caches.withinTx(region.getTransactionManager(), new Callable<Void>() {
               @Override
               public Void call() throws Exception {
                  region.getCache().withFlags(Flag.CACHE_MODE_LOCAL).clear();
                  return null;
               }
            });
				region.getCache().stop();
			}
			entityRegionMap.clear();
		}
		if ( collectionRegionMap != null ) {
			for ( final CollectionRegionImpl collectionRegion : collectionRegionMap.values() ) {
            Caches.withinTx(collectionRegion.getTransactionManager(), new Callable<Void>() {
               @Override
               public Void call() throws Exception {
                  collectionRegion.getCache().withFlags(Flag.CACHE_MODE_LOCAL).clear();
                  return null;
               }
            });
				collectionRegion.getCache().stop();
			}
			collectionRegionMap.clear();
		}
		if ( regionFactory != null ) {
// Currently the RegionFactory is shutdown by its registration with the CacheTestSetup from CacheTestUtil when built
			regionFactory.stop();
		}
		if ( serviceRegistry != null ) {
			serviceRegistry.destroy();
		}
	}
}
