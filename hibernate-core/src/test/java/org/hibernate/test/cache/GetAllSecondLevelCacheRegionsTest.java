/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.internal.StandardQueryCache;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class GetAllSecondLevelCacheRegionsTest extends BaseCoreFunctionalTestCase {
	private static String PREFIX = "HIBERNATE";

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { AnEntity.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( AvailableSettings.CACHE_REGION_PREFIX, PREFIX );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12233")
	public void testQueryCacheDisabled() {
		Map properties = new HashMap();
		properties.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		properties.put( AvailableSettings.USE_QUERY_CACHE, "false" );

		rebuildSessionFactory( properties );

		Map regions = sessionFactory().getAllSecondLevelCacheRegions();

		assertEquals( 2, regions.size() );

		assertNull( regions.get( getCacheRegionName( StandardQueryCache.class.getName() ) ) );
		assertNull( regions.get( getCacheRegionName( UpdateTimestampsCache.class.getName() ) ) );
		assertNotNull( regions.get( getCacheRegionName( AnEntity.class.getName() ) ) );
		assertNotNull( regions.get( getCacheRegionName( AnEntity.class.getName() + ".strings" ) ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12233")
	public void testQueryCacheEnabled() {
		Map properties = new HashMap();
		properties.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		properties.put( AvailableSettings.USE_QUERY_CACHE, "true" );

		rebuildSessionFactory( properties );

		Map regions = sessionFactory().getAllSecondLevelCacheRegions();

		assertEquals( 4, regions.size() );

		assertNotNull( regions.get( getCacheRegionName( StandardQueryCache.class.getName() ) ) );
		assertNotNull( regions.get( getCacheRegionName( UpdateTimestampsCache.class.getName() ) ) );
		assertNotNull( regions.get( getCacheRegionName( AnEntity.class.getName() ) ) );
		assertNotNull( regions.get( getCacheRegionName( AnEntity.class.getName() + ".strings" ) ) );

		Session s = openSession();
		s.getTransaction().begin();
		s.createQuery( "from AnEntity" ).setCacheable( true ).setCacheRegion( "ACacheRegion" ).list();
		s.getTransaction().commit();
		s.close();

		regions = sessionFactory().getAllSecondLevelCacheRegions();

		assertEquals( 5, regions.size() );

		assertNotNull( regions.get( getCacheRegionName( "ACacheRegion" ) ) );

		assertNotNull( regions.get( getCacheRegionName( StandardQueryCache.class.getName() ) ) );
		assertNotNull( regions.get( getCacheRegionName( UpdateTimestampsCache.class.getName() ) ) );
		assertNotNull( regions.get( getCacheRegionName( AnEntity.class.getName() ) ) );
		assertNotNull( regions.get( getCacheRegionName( AnEntity.class.getName() + ".strings" ) ) );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12233")
	public void testSecondLevelCacheDisabled() {
		Map properties = new HashMap();
		properties.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );

		rebuildSessionFactory( properties );

		Map regions = sessionFactory().getAllSecondLevelCacheRegions();

		assertTrue( regions.isEmpty() );
	}


	private String getCacheRegionName(String role) {
		return PREFIX + '.' + role;
	}

	@Entity(name = "AnEntity")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AnEntity {
		@Id
		@GeneratedValue
		private long id;

		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		@ElementCollection
		private Set<String> strings;
	}
}