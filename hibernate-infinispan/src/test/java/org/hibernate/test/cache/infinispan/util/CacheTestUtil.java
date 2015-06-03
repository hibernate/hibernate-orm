/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.internal.util.compare.EqualsHelper;
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
         final Properties properties = toProperties( cfgService.getSettings() );

         regionFactory.start( sessionFactoryOptions, properties );

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
    * Executes {@link #assertEqualsEventually(Object, Callable, long, TimeUnit)} without time limit.
    * @param expected
    * @param callable
    * @param <T>
    */
   public static <T> void assertEqualsEventually(T expected, Callable<T> callable) throws Exception {
      assertEqualsEventually(expected, callable, -1, TimeUnit.SECONDS);
   }

   /**
    * Periodically calls callable and compares returned value with expected value. If the value matches to expected,
    * the method returns. If callable throws an exception, this is propagated. If the returned value does not match to
    * expected before timeout, {@link TimeoutException} is thrown.
    * @param expected
    * @param callable
    * @param timeout If non-positive, there is no limit.
    * @param timeUnit
    * @param <T>
    */
   public static <T> void assertEqualsEventually(T expected, Callable<T> callable, long timeout, TimeUnit timeUnit) throws Exception {
      long now, deadline = timeout <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + timeUnit.toMillis(timeout);
      for (;;) {
         T value = callable.call();
         if (EqualsHelper.equals(value, expected)) return;
         now = System.currentTimeMillis();
         if (now < deadline) {
            Thread.sleep(Math.min(100, deadline - now));
         } else break;
      }
      throw new TimeoutException();
   }

   /**
    * Prevent instantiation.
    */
   private CacheTestUtil() {
   }
}
