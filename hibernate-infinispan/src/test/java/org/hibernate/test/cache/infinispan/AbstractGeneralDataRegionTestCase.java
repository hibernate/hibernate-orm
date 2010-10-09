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
package org.hibernate.test.cache.infinispan;

import java.util.Set;

import org.hibernate.cache.GeneralDataRegion;
import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.Region;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;
import org.infinispan.transaction.tm.BatchModeTransactionManager;

/**
 * Base class for tests of QueryResultsRegion and TimestampsRegion.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractGeneralDataRegionTestCase extends AbstractRegionImplTestCase {
   protected static final String KEY = "Key";

   protected static final String VALUE1 = "value1";
   protected static final String VALUE2 = "value2";

   public AbstractGeneralDataRegionTestCase(String name) {
      super(name);
   }

   @Override
   protected void putInRegion(Region region, Object key, Object value) {
      ((GeneralDataRegion) region).put(key, value);
   }

   @Override
   protected void removeFromRegion(Region region, Object key) {
      ((GeneralDataRegion) region).evict(key);
   }

   /**
    * Test method for {@link QueryResultsRegion#evict(java.lang.Object)}.
    * 
    * FIXME add testing of the "immediately without regard for transaction isolation" bit in the
    * CollectionRegionAccessStrategy API.
    */
   public void testEvict() throws Exception {
      evictOrRemoveTest();
   }

   private void evictOrRemoveTest() throws Exception {
      Configuration cfg = createConfiguration();
      InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
      CacheAdapter localCache = getInfinispanCache(regionFactory);
      boolean invalidation = localCache.isClusteredInvalidation();

      // Sleep a bit to avoid concurrent FLUSH problem
      avoidConcurrentFlush();

      GeneralDataRegion localRegion = (GeneralDataRegion) createRegion(regionFactory,
               getStandardRegionName(REGION_PREFIX), cfg.getProperties(), null);

      cfg = createConfiguration();
      regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());

      GeneralDataRegion remoteRegion = (GeneralDataRegion) createRegion(regionFactory,
               getStandardRegionName(REGION_PREFIX), cfg.getProperties(), null);

      assertNull("local is clean", localRegion.get(KEY));
      assertNull("remote is clean", remoteRegion.get(KEY));

      localRegion.put(KEY, VALUE1);
      assertEquals(VALUE1, localRegion.get(KEY));

      // allow async propagation
      sleep(250);
      Object expected = invalidation ? null : VALUE1;
      assertEquals(expected, remoteRegion.get(KEY));

      localRegion.evict(KEY);

      // allow async propagation
      sleep(250);
      assertEquals(null, localRegion.get(KEY));
      assertEquals(null, remoteRegion.get(KEY));
   }

   protected abstract String getStandardRegionName(String regionPrefix);

   /**
    * Test method for {@link QueryResultsRegion#evictAll()}.
    * 
    * FIXME add testing of the "immediately without regard for transaction isolation" bit in the
    * CollectionRegionAccessStrategy API.
    */
   public void testEvictAll() throws Exception {
      evictOrRemoveAllTest("entity");
   }

   private void evictOrRemoveAllTest(String configName) throws Exception {
      Configuration cfg = createConfiguration();
      InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
      CacheAdapter localCache = getInfinispanCache(regionFactory);

      // Sleep a bit to avoid concurrent FLUSH problem
      avoidConcurrentFlush();

      GeneralDataRegion localRegion = (GeneralDataRegion) createRegion(regionFactory,
               getStandardRegionName(REGION_PREFIX), cfg.getProperties(), null);

      cfg = createConfiguration();
      regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
      CacheAdapter remoteCache = getInfinispanCache(regionFactory);

      // Sleep a bit to avoid concurrent FLUSH problem
      avoidConcurrentFlush();

      GeneralDataRegion remoteRegion = (GeneralDataRegion) createRegion(regionFactory,
               getStandardRegionName(REGION_PREFIX), cfg.getProperties(), null);

      Set keys = localCache.keySet();
      assertEquals("No valid children in " + keys, 0, getValidKeyCount(keys));

      keys = remoteCache.keySet();
      assertEquals("No valid children in " + keys, 0, getValidKeyCount(keys));

      assertNull("local is clean", localRegion.get(KEY));
      assertNull("remote is clean", remoteRegion.get(KEY));

      localRegion.put(KEY, VALUE1);
      assertEquals(VALUE1, localRegion.get(KEY));

      // Allow async propagation
      sleep(250);

      remoteRegion.put(KEY, VALUE1);
      assertEquals(VALUE1, remoteRegion.get(KEY));

      // Allow async propagation
      sleep(250);

      localRegion.evictAll();

      // allow async propagation
      sleep(250);
      // This should re-establish the region root node in the optimistic case
      assertNull(localRegion.get(KEY));
      assertEquals("No valid children in " + keys, 0, getValidKeyCount(localCache.keySet()));

      // Re-establishing the region root on the local node doesn't
      // propagate it to other nodes. Do a get on the remote node to re-establish
      // This only adds a node in the case of optimistic locking
      assertEquals(null, remoteRegion.get(KEY));
      assertEquals("No valid children in " + keys, 0, getValidKeyCount(remoteCache.keySet()));

      assertEquals("local is clean", null, localRegion.get(KEY));
      assertEquals("remote is clean", null, remoteRegion.get(KEY));
   }

   protected Configuration createConfiguration() {
      Configuration cfg = CacheTestUtil.buildConfiguration("test", InfinispanRegionFactory.class, false, true);
      return cfg;
   }

   protected void rollback() {
      try {
         BatchModeTransactionManager.getInstance().rollback();
      } catch (Exception e) {
         log.error(e.getMessage(), e);
      }
   }
}