/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan;

import java.util.Properties;

import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.Region;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.infinispan.AdvancedCache;

/**
 * Base class for tests of Region implementations.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public abstract class AbstractRegionImplTest extends AbstractNonFunctionalTest {

   protected abstract AdvancedCache getInfinispanCache(InfinispanRegionFactory regionFactory);

   protected abstract Region createRegion(InfinispanRegionFactory regionFactory, String regionName, Properties properties, CacheDataDescription cdd);

   protected abstract void putInRegion(Region region, Object key, Object value);

   protected abstract void removeFromRegion(Region region, Object key);

   protected CacheDataDescription getCacheDataDescription() {
      return new CacheDataDescriptionImpl(true, true, ComparableComparator.INSTANCE, null);
   }

}
