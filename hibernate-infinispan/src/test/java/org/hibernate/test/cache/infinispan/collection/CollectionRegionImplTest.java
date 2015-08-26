/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.test.cache.infinispan.AbstractEntityCollectionRegionTest;
import org.infinispan.AdvancedCache;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Galder Zamarreño
 */
public class CollectionRegionImplTest extends AbstractEntityCollectionRegionTest {
	@Override
	protected void supportedAccessTypeTest(RegionFactory regionFactory, Properties properties) {
		CollectionRegion region = regionFactory.buildCollectionRegion("test", properties, MUTABLE_NON_VERSIONED);
		assertNotNull(region.buildAccessStrategy(AccessType.READ_ONLY));
		assertNotNull(region.buildAccessStrategy(AccessType.READ_WRITE));
		assertNotNull(region.buildAccessStrategy(AccessType.TRANSACTIONAL));
		try {
			region.buildAccessStrategy(AccessType.NONSTRICT_READ_WRITE);
			fail("Incorrectly got NONSTRICT_READ_WRITE");
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
		strategy.putFromLoad(null, key, value, System.currentTimeMillis(), new Integer(1));
	}

	@Override
	protected void removeFromRegion(Region region, Object key) {
		((CollectionRegion) region).buildAccessStrategy(AccessType.TRANSACTIONAL).remove(null, key);
	}

}
