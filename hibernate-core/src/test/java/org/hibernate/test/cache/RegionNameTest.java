/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import java.util.List;
import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.CacheImplementor;
import org.hibernate.stat.NaturalIdCacheStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test API and SPI expectation wrt region names - whether they expect the
 * prefixed or un-prefixed name
 *
 * @author Steve Ebersole
 */
public class RegionNameTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Person.class );
	}

	private final String cachePrefix = "app1";

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( AvailableSettings.CACHE_REGION_PREFIX, cachePrefix );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
	}

	// todo (5.3) : any other API I can think of that deals with region-name?
	// todo (6.0) : same ^^, maintain API compatibility

	@Test
	public void testStatsApi() {
		//this needs to be the prefixed name :(
		//final String regionName = "a.b.c";
		final String regionName = cachePrefix + ".a.b.c";

		final Statistics stats = sessionFactory().getStatistics();

		final SecondLevelCacheStatistics secondLevelCacheStatistics = stats.getSecondLevelCacheStatistics( regionName);
		assert secondLevelCacheStatistics != null;

		final NaturalIdCacheStatistics naturalIdCacheStatistics = stats.getNaturalIdCacheStatistics( regionName );
		assert naturalIdCacheStatistics != null;
	}

	// todo (5.3) : any other API I can think of that deals with region-name?
	// todo (6.0) : same ^^, maintain API compatibility

	@Test
	public void testStatsSpi() {
		//this needs to be the prefixed name :(
		//final String regionName = "a.b.c";
		final String regionName = cachePrefix + ".a.b.c";

		final StatisticsImplementor statistics = sessionFactory().getStatistics();

		statistics.naturalIdCacheHit(regionName);
		statistics.naturalIdCacheMiss( regionName );
		statistics.naturalIdCachePut( regionName );

		statistics.secondLevelCacheHit( regionName );
		statistics.secondLevelCacheMiss( regionName );
		statistics.secondLevelCachePut( regionName );

		// stats for queries cannot be accessed second level cache regions map
//		final String queryString = "select p from Person p";
//		final String queryCacheRegionName = "x.y.z";
//
//		inTransaction(
//				session -> session.createQuery( queryString ).setCacheable( true ).setCacheRegion( queryCacheRegionName ).list()
//		);
//		// note that
//		statistics.queryCacheHit( "select ...", queryCacheRegionName );
//		statistics.queryCacheMiss( "select ...", queryCacheRegionName );
//		statistics.queryCachePut( "select ...", queryCacheRegionName );
	}

	@Test
	public void testCacheSpi() {
		//this needs to be the prefixed name :(
		//final String regionName = "a.b.c";
		final String regionName = cachePrefix + ".a.b.c";

		final CacheImplementor cache = sessionFactory().getCache();

		// just like stats, the cache for queries cannot be accessed second level cache regions map
		assert cache.getSecondLevelCacheRegionNames().length == 1;
		assert cache.getSecondLevelCacheRegionNames()[0].equals( regionName );

		assert cache.getEntityRegionAccess( regionName ) != null;
		assert cache.getNaturalIdCacheRegionAccessStrategy( regionName ) != null;
		assert cache.getCollectionRegionAccess(regionName ) != null;
	}

	@Entity( name = "Person" )
	@Table( name = "persons" )
	@Cacheable
	@Cache( region = "a.b.c", usage = CacheConcurrencyStrategy.READ_WRITE )
	@NaturalIdCache( region = "a.b.c" )
	public static class Person {
		@Id
		public Integer id;

		@NaturalId
		public String name;

		@ElementCollection
		@Cache( region = "a.b.c", usage = CacheConcurrencyStrategy.READ_WRITE )
		public List<String> nickNames;
	}

}
