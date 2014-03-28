/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
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
package org.hibernate.test.cache.infinispan.util;

import java.util.Properties;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Settings;
import org.hibernate.cfg.SettingsFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.test.cache.infinispan.functional.SingleNodeTestCase;

/**
 * Utilities for cache testing.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public class CacheTestUtil {

   public static Configuration buildConfiguration(String regionPrefix,
         Class regionFactory, boolean use2ndLevel, boolean useQueries) {
      Configuration cfg = new Configuration();
      cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
      cfg.setProperty(Environment.USE_STRUCTURED_CACHE, "true");
      cfg.setProperty( AvailableSettings.JTA_PLATFORM, BatchModeJtaPlatform.class.getName() );

      cfg.setProperty(Environment.CACHE_REGION_FACTORY, regionFactory.getName());
      cfg.setProperty(Environment.CACHE_REGION_PREFIX, regionPrefix);
      cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, String.valueOf(use2ndLevel));
      cfg.setProperty(Environment.USE_QUERY_CACHE, String.valueOf(useQueries));

      return cfg;
   }

   public static Configuration buildCustomQueryCacheConfiguration(String regionPrefix, String queryCacheName) {
      Configuration cfg = buildConfiguration(regionPrefix, InfinispanRegionFactory.class, true, true);
      cfg.setProperty(InfinispanRegionFactory.QUERY_CACHE_RESOURCE_PROP, queryCacheName);
      return cfg;
   }

   public static InfinispanRegionFactory startRegionFactory(ServiceRegistry reg,
         Configuration cfg){
      try {
         Properties properties = cfg.getProperties();
         Settings settings = new SettingsFactory().buildSettings( properties, reg );

         String factoryType = cfg.getProperty(Environment.CACHE_REGION_FACTORY);
         Class clazz = Thread.currentThread()
               .getContextClassLoader().loadClass(factoryType);
         InfinispanRegionFactory regionFactory;
         if (clazz == InfinispanRegionFactory.class) {
            regionFactory = new SingleNodeTestCase.TestInfinispanRegionFactory();
         } else {
            regionFactory = (InfinispanRegionFactory) clazz.newInstance();
         }
         regionFactory.start(settings, properties);
         return regionFactory;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static InfinispanRegionFactory startRegionFactory(ServiceRegistry reg,
         Configuration cfg, CacheTestSupport testSupport) {
      InfinispanRegionFactory factory = startRegionFactory(reg, cfg);
      testSupport.registerFactory(factory);
      return factory;
   }

   public static void stopRegionFactory(InfinispanRegionFactory factory,
         CacheTestSupport testSupport) {
      factory.stop();
      testSupport.unregisterFactory(factory);
   }

   /**
    * Prevent instantiation.
    */
   private CacheTestUtil() {
   }

}
