/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import net.sf.ehcache.CacheManager;
import org.junit.BeforeClass;
import org.junit.Test;

import org.hibernate.cache.ehcache.management.impl.EhcacheStatsImpl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class EhcacheStatsImplTest {

	private static EhcacheStatsImpl stats;

	@BeforeClass
	public static void createCache() throws Exception {
		CacheManager manager = CacheManager.getInstance();
		stats = new EhcacheStatsImpl( manager );
	}

	@Test
	public void testIsRegionCacheOrphanEvictionEnabled() {
		assertThat( stats.isRegionCacheOrphanEvictionEnabled( "sampleCache1" ), is( false ) );
	}

	@Test
	public void testGetRegionCacheOrphanEvictionPeriod() {
		assertThat( stats.getRegionCacheOrphanEvictionPeriod( "sampleCache1" ), is( -1 ) );
	}
}