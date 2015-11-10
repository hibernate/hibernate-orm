/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.entity;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.test.cache.infinispan.AbstractEntityCollectionRegionTest;
import org.infinispan.AdvancedCache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Tests of EntityRegionImpl.
 * 
 * @author Galder Zamarreño
 * @since 3.5
 */
public class EntityRegionImplTest extends AbstractEntityCollectionRegionTest {
	protected static final String CACHE_NAME = "test";

	@Override
	protected void supportedAccessTypeTest(RegionFactory regionFactory, Properties properties) {
		EntityRegion region = regionFactory.buildEntityRegion("test", properties, MUTABLE_NON_VERSIONED);
		assertNotNull(region.buildAccessStrategy(accessType));
		((InfinispanRegionFactory) regionFactory).getCacheManager().removeCache(CACHE_NAME);
	}

	@Override
	protected void putInRegion(Region region, Object key, Object value) {
		((EntityRegion) region).buildAccessStrategy(AccessType.TRANSACTIONAL).insert(null, key, value, 1);
	}

	@Override
	protected void removeFromRegion(Region region, Object key) {
		((EntityRegion) region).buildAccessStrategy(AccessType.TRANSACTIONAL).remove(null, key);
	}

	@Override
	protected Region createRegion(InfinispanRegionFactory regionFactory, String regionName, Properties properties, CacheDataDescription cdd) {
		return regionFactory.buildEntityRegion(regionName, properties, cdd);
	}

	@Override
	protected AdvancedCache getInfinispanCache(InfinispanRegionFactory regionFactory) {
		return regionFactory.getCacheManager().getCache(
				InfinispanRegionFactory.DEF_ENTITY_RESOURCE).getAdvancedCache();
	}

}
