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

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.Settings;
import org.infinispan.manager.CacheManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * ClusterAwareRegionFactory.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class ClusterAwareRegionFactory implements RegionFactory {
   
   private static final Log log = LogFactory.getLog(ClusterAwareRegionFactory.class);
   private static final Hashtable<String, CacheManager> cacheManagers = new Hashtable<String, CacheManager>();

   private final InfinispanRegionFactory delegate = new InfinispanRegionFactory();
   private String cacheManagerName;
   private boolean locallyAdded;
   
   public ClusterAwareRegionFactory(Properties props) {
   }
   
   public static CacheManager getCacheManager(String name) {
      return cacheManagers.get(name);
   }
   
   public static void addCacheManager(String name, CacheManager manager) {
      cacheManagers.put(name, manager);
   }
   
   public static void clearCacheManagers() {
      for (CacheManager manager : cacheManagers.values()) {
         try {
            manager.stop();
         } catch (Exception e) {
            log.error("Exception cleaning up CacheManager " + manager, e);
         }
      }
      cacheManagers.clear();      
   }

   public void start(Settings settings, Properties properties) throws CacheException {
      cacheManagerName = properties.getProperty(DualNodeTestCase.NODE_ID_PROP);
      
      CacheManager existing = getCacheManager(cacheManagerName);
      locallyAdded = (existing == null);
      
      if (locallyAdded) {
         delegate.start(settings, properties);
         cacheManagers.put(cacheManagerName, delegate.getCacheManager());
      } else {
         delegate.setCacheManager(existing);
      }      
   }

   public void stop() {
      if (locallyAdded) cacheManagers.remove(cacheManagerName);     
      delegate.stop();
   }

   public CollectionRegion buildCollectionRegion(String regionName, Properties properties,
            CacheDataDescription metadata) throws CacheException {
      return delegate.buildCollectionRegion(regionName, properties, metadata);
   }

   public EntityRegion buildEntityRegion(String regionName, Properties properties,
            CacheDataDescription metadata) throws CacheException {
      return delegate.buildEntityRegion(regionName, properties, metadata);
   }

   public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties)
            throws CacheException {
      return delegate.buildQueryResultsRegion(regionName, properties);
   }

   public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties)
            throws CacheException {
      return delegate.buildTimestampsRegion(regionName, properties);
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
