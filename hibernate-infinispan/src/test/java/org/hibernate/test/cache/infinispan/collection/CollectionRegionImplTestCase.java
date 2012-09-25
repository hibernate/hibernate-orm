/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.cache.infinispan.collection;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.test.cache.infinispan.AbstractEntityCollectionRegionTestCase;
import org.infinispan.AdvancedCache;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests of CollectionRegionImpl.
 * 
 * @author Galder Zamarreño
 */
public class CollectionRegionImplTestCase extends AbstractEntityCollectionRegionTestCase {
   @Override
   protected void supportedAccessTypeTest(RegionFactory regionFactory, Properties properties) {
      CollectionRegion region = regionFactory.buildCollectionRegion("test", properties, null);
      assertNull("Got TRANSACTIONAL", region.buildAccessStrategy(AccessType.TRANSACTIONAL)
               .lockRegion());
      try {
         region.buildAccessStrategy(AccessType.NONSTRICT_READ_WRITE);
         fail("Incorrectly got NONSTRICT_READ_WRITE");
      } catch (CacheException good) {
      }

      try {
         region.buildAccessStrategy(AccessType.READ_WRITE);
         fail("Incorrectly got READ_WRITE");
      } catch (CacheException good) {
      }
   }

   @Override
   protected Region createRegion(InfinispanRegionFactory regionFactory, String regionName, Properties properties, CacheDataDescription cdd) {
      return regionFactory.buildCollectionRegion(regionName, properties, cdd);
   }

   @Override
   protected AdvancedCache getInfinispanCache(InfinispanRegionFactory regionFactory) {
      return regionFactory.getCacheManager().getCache(InfinispanRegionFactory.DEF_ENTITY_RESOURCE).getAdvancedCache();
   }

   @Override
   protected void putInRegion(Region region, Object key, Object value) {
      CollectionRegionAccessStrategy strategy = ((CollectionRegion) region).buildAccessStrategy(AccessType.TRANSACTIONAL);
      strategy.putFromLoad(key, value, System.currentTimeMillis(), new Integer(1));
   }

   @Override
   protected void removeFromRegion(Region region, Object key) {
      ((CollectionRegion) region).buildAccessStrategy(AccessType.TRANSACTIONAL).remove(key);
   }

}
