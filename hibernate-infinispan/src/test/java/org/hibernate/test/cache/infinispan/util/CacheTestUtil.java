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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.test.cache.infinispan.functional.SingleNodeTestCase;

/**
 * Utilities for cache testing.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public class CacheTestUtil {
   @SuppressWarnings("unchecked")
   public static Map buildBaselineSettings(
           String regionPrefix,
           Class regionFactory,
           boolean use2ndLevel,
           boolean useQueries) {
      Map settings = new HashMap();

      settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
      settings.put( AvailableSettings.USE_STRUCTURED_CACHE, "true" );
      settings.put( AvailableSettings.JTA_PLATFORM, BatchModeJtaPlatform.class.getName() );

      settings.put( AvailableSettings.CACHE_REGION_FACTORY, regionFactory.getName() );
      settings.put( AvailableSettings.CACHE_REGION_PREFIX, regionPrefix );
      settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, String.valueOf( use2ndLevel ) );
      settings.put( AvailableSettings.USE_QUERY_CACHE, String.valueOf( useQueries ) );

      return settings;
   }

   public static StandardServiceRegistryBuilder buildBaselineStandardServiceRegistryBuilder(
           String regionPrefix,
           Class regionFactory,
           boolean use2ndLevel,
           boolean useQueries) {
      StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();

      ssrb.applySettings(
              buildBaselineSettings( regionPrefix, regionFactory, use2ndLevel, useQueries )
      );

      return ssrb;
   }

   public static StandardServiceRegistryBuilder buildCustomQueryCacheStandardServiceRegistryBuilder(
           String regionPrefix,
           String queryCacheName) {
      final StandardServiceRegistryBuilder ssrb = buildBaselineStandardServiceRegistryBuilder(
              regionPrefix, InfinispanRegionFactory.class, true, true
      );
      ssrb.applySetting( InfinispanRegionFactory.QUERY_CACHE_RESOURCE_PROP, queryCacheName );
      return ssrb;
   }

   public static InfinispanRegionFactory startRegionFactory(ServiceRegistry serviceRegistry) {
      try {
         final ConfigurationService cfgService = serviceRegistry.getService( ConfigurationService.class );

         String factoryType = cfgService.getSetting( AvailableSettings.CACHE_REGION_FACTORY, StandardConverters.STRING );
         Class clazz = Thread.currentThread().getContextClassLoader().loadClass( factoryType );
         InfinispanRegionFactory regionFactory;
         if (clazz == InfinispanRegionFactory.class) {
            regionFactory = new SingleNodeTestCase.TestInfinispanRegionFactory();
         }
         else {
            regionFactory = (InfinispanRegionFactory) clazz.newInstance();
         }

         final SessionFactoryOptionsImpl sessionFactoryOptions = new SessionFactoryOptionsImpl(
                 new SessionFactoryBuilderImpl.SessionFactoryOptionsStateStandardImpl(
                         (StandardServiceRegistry) serviceRegistry
                 )
         );
         final Settings settings = new Settings( sessionFactoryOptions );
         final Properties properties = toProperties( cfgService.getSettings() );

         regionFactory.start( settings, properties );

         return regionFactory;
      }
      catch (RuntimeException e) {
         throw e;
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public static InfinispanRegionFactory startRegionFactory(
           ServiceRegistry serviceRegistry,
           CacheTestSupport testSupport) {
      InfinispanRegionFactory factory = startRegionFactory( serviceRegistry );
      testSupport.registerFactory( factory );
      return factory;
   }

   public static void stopRegionFactory(
           InfinispanRegionFactory factory,
           CacheTestSupport testSupport) {
      testSupport.unregisterFactory( factory );
      factory.stop();
   }

   public static Properties toProperties(Map map) {
      if ( map == null ) {
         return null;
      }

      if ( map instanceof Properties ) {
         return (Properties) map;
      }

      Properties properties = new Properties();
      properties.putAll( map );
      return properties;
   }

   /**
    * Prevent instantiation.
    */
   private CacheTestUtil() {
   }

}
