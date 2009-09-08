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

import java.util.Properties;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.TransactionalDataRegion;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.cache.infinispan.util.CacheTestUtil;

/**
 * Base class for tests of EntityRegion and CollectionRegion implementations.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractEntityCollectionRegionTestCase extends AbstractRegionImplTestCase {

   /**
    * Create a new EntityCollectionRegionTestCaseBase.
    * 
    * @param name
    */
   public AbstractEntityCollectionRegionTestCase(String name) {
      super(name);
   }

   /**
    * Creates a Region backed by an PESSIMISTIC locking JBoss Cache, and then ensures that it
    * handles calls to buildAccessStrategy as expected when all the various {@link AccessType}s are
    * passed as arguments.
    */
   public void testSupportedAccessTypes() throws Exception {
      supportedAccessTypeTest();
   }

   private void supportedAccessTypeTest() throws Exception {
      Configuration cfg = CacheTestUtil.buildConfiguration("test", InfinispanRegionFactory.class, true, false);
      String entityCfg = "entity";
      cfg.setProperty(InfinispanRegionFactory.ENTITY_CACHE_RESOURCE_PROP, entityCfg);
      InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
      supportedAccessTypeTest(regionFactory, cfg.getProperties());
   }

   /**
    * Creates a Region using the given factory, and then ensure that it handles calls to
    * buildAccessStrategy as expected when all the various {@link AccessType}s are passed as
    * arguments.
    */
   protected abstract void supportedAccessTypeTest(RegionFactory regionFactory, Properties properties);

   /**
    * Test that the Region properly implements {@link TransactionalDataRegion#isTransactionAware()}.
    * 
    * @throws Exception
    */
   public void testIsTransactionAware() throws Exception {
      Configuration cfg = CacheTestUtil.buildConfiguration("test", InfinispanRegionFactory.class, true, false);
      InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
      TransactionalDataRegion region = (TransactionalDataRegion) createRegion(regionFactory, "test/test", cfg.getProperties(), getCacheDataDescription());
      assertTrue("Region is transaction-aware", region.isTransactionAware());
      CacheTestUtil.stopRegionFactory(regionFactory, getCacheTestSupport());
      cfg = CacheTestUtil.buildConfiguration("test", InfinispanRegionFactory.class, true, false);
      // Make it non-transactional
      cfg.getProperties().remove(Environment.TRANSACTION_MANAGER_STRATEGY);
      regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
      region = (TransactionalDataRegion) createRegion(regionFactory, "test/test", cfg.getProperties(), getCacheDataDescription());
      assertFalse("Region is not transaction-aware", region.isTransactionAware());
      CacheTestUtil.stopRegionFactory(regionFactory, getCacheTestSupport());
   }

   public void testGetCacheDataDescription() throws Exception {
      Configuration cfg = CacheTestUtil.buildConfiguration("test", InfinispanRegionFactory.class, true, false);
      InfinispanRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
      TransactionalDataRegion region = (TransactionalDataRegion) createRegion(regionFactory, "test/test", cfg.getProperties(), getCacheDataDescription());
      CacheDataDescription cdd = region.getCacheDataDescription();
      assertNotNull(cdd);
      CacheDataDescription expected = getCacheDataDescription();
      assertEquals(expected.isMutable(), cdd.isMutable());
      assertEquals(expected.isVersioned(), cdd.isVersioned());
      assertEquals(expected.getVersionComparator(), cdd.getVersionComparator());
   }
}
