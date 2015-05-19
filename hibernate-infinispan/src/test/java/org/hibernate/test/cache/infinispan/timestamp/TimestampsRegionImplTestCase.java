/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.timestamp;

import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.timestamp.TimestampsRegionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.UpdateTimestampsCache;

import org.hibernate.test.cache.infinispan.AbstractGeneralDataRegionTestCase;
import org.hibernate.test.cache.infinispan.functional.SingleNodeTestCase;
import org.hibernate.test.cache.infinispan.functional.classloader.Account;
import org.hibernate.test.cache.infinispan.functional.classloader.AccountHolder;
import org.hibernate.test.cache.infinispan.functional.classloader.SelectedClassnameClassLoader;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.hibernate.test.cache.infinispan.util.ClassLoaderAwareCache;

import org.infinispan.AdvancedCache;
import org.infinispan.context.Flag;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryActivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryInvalidated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryLoaded;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryPassivated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.Event;

/**
 * Tests of TimestampsRegionImpl.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class TimestampsRegionImplTestCase extends AbstractGeneralDataRegionTestCase {

    @Override
   protected String getStandardRegionName(String regionPrefix) {
      return regionPrefix + "/" + UpdateTimestampsCache.class.getName();
   }

   @Override
   protected Region createRegion(InfinispanRegionFactory regionFactory, String regionName, Properties properties, CacheDataDescription cdd) {
      return regionFactory.buildTimestampsRegion(regionName, properties);
   }

   @Override
   protected AdvancedCache getInfinispanCache(InfinispanRegionFactory regionFactory) {
      return regionFactory.getCacheManager().getCache("timestamps").getAdvancedCache();
   }

   public void testClearTimestampsRegionInIsolated() throws Exception {
      StandardServiceRegistryBuilder ssrb = createStandardServiceRegistryBuilder();
      final StandardServiceRegistry registry = ssrb.build();
      final StandardServiceRegistry registry2 = ssrb.build();

      try {
         final Properties properties = CacheTestUtil.toProperties( ssrb.getSettings() );

         InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(
                 registry,
                 getCacheTestSupport()
         );
         // Sleep a bit to avoid concurrent FLUSH problem
         avoidConcurrentFlush();

         InfinispanRegionFactory regionFactory2 = CacheTestUtil.startRegionFactory(
                 registry2,
                 getCacheTestSupport()
         );
         // Sleep a bit to avoid concurrent FLUSH problem
         avoidConcurrentFlush();

         TimestampsRegionImpl region = (TimestampsRegionImpl) regionFactory.buildTimestampsRegion(
                 getStandardRegionName(REGION_PREFIX),
                 properties
         );
         TimestampsRegionImpl region2 = (TimestampsRegionImpl) regionFactory2.buildTimestampsRegion(
                 getStandardRegionName(REGION_PREFIX),
                 properties
         );

         Account acct = new Account();
         acct.setAccountHolder(new AccountHolder());
         region.getCache().withFlags(Flag.FORCE_SYNCHRONOUS).put(acct, "boo");
      }
      finally {
         StandardServiceRegistryBuilder.destroy( registry );
         StandardServiceRegistryBuilder.destroy( registry2 );
      }
   }

   @Override
   protected StandardServiceRegistryBuilder createStandardServiceRegistryBuilder() {
      return CacheTestUtil.buildBaselineStandardServiceRegistryBuilder(
              "test",
              MockInfinispanRegionFactory.class,
              false,
              true
      );
   }

   public static class MockInfinispanRegionFactory extends SingleNodeTestCase.TestInfinispanRegionFactory {

      public MockInfinispanRegionFactory() {
      }

      @Override
      protected AdvancedCache createCacheWrapper(AdvancedCache cache) {
         return new ClassLoaderAwareCache(cache, Thread.currentThread().getContextClassLoader()) {
            @Override
            public void addListener(Object listener) {
               super.addListener(new MockClassLoaderAwareListener(listener, this));
            }
         };
      }

      @Listener
      public static class MockClassLoaderAwareListener extends ClassLoaderAwareCache.ClassLoaderAwareListener {
         MockClassLoaderAwareListener(Object listener, ClassLoaderAwareCache cache) {
            super(listener, cache);
         }

         @CacheEntryActivated
         @CacheEntryCreated
         @CacheEntryEvicted
         @CacheEntryInvalidated
         @CacheEntryLoaded
         @CacheEntryModified
         @CacheEntryPassivated
         @CacheEntryRemoved
         @CacheEntryVisited
         public void event(Event event) throws Throwable {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            String notFoundPackage = "org.hibernate.test.cache.infinispan.functional.classloader";
            String[] notFoundClasses = { notFoundPackage + ".Account", notFoundPackage + ".AccountHolder" };
            SelectedClassnameClassLoader visible = new SelectedClassnameClassLoader(null, null, notFoundClasses, cl);
            Thread.currentThread().setContextClassLoader(visible);
            super.event(event);
            Thread.currentThread().setContextClassLoader(cl);
         }
      }
   }

}
