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
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.stat.NaturalIdCacheStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
	private final String localName = "a.b.c";
	private final String prefixedName = cachePrefix + '.' + localName;

	private final NavigableRole personRole = new NavigableRole( Person.class.getName() );
	private final NavigableRole personNameRole = personRole;
	private final NavigableRole personNickNamesRole = personRole.append( "nickNames" );

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( AvailableSettings.USE_QUERY_CACHE, "true" );
		settings.put( AvailableSettings.CACHE_REGION_PREFIX, cachePrefix );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
	}

	// todo (5.3) : any other API I can think of that deals with region-name?
	// todo (6.0) : same ^^, maintain API compatibility

	@Test
	public void testLegacyStatsApi() {
		// these references need to be the prefixed name
		final String regionName = cachePrefix + '.' + localName;

		final Statistics stats = sessionFactory().getStatistics();

		assertEquals( 2, stats.getSecondLevelCacheRegionNames().length );

		final SecondLevelCacheStatistics secondLevelCacheStatistics = stats.getSecondLevelCacheStatistics( regionName );
		assert secondLevelCacheStatistics != null;

		final NaturalIdCacheStatistics naturalIdCacheStatistics = stats.getNaturalIdCacheStatistics( regionName );
		assert naturalIdCacheStatistics != null;

		final SecondLevelCacheStatistics dne = stats.getSecondLevelCacheStatistics( cachePrefix + ".does.not.exist" );
		assert dne != null;
	}

	// todo (5.3) : any other API I can think of that deals with region-name?
	// todo (6.0) : same ^^, maintain API compatibility

	@Test
	public void testLegacyStatsSpi() {
		// NOTE : these calls actually did change - the ability to provide
		// some better stats to the user

//		// these need to be the prefixed name
//		final String regionName = cachePrefix + '.' + localName;
		final String regionName = localName;

		final StatisticsImplementor statistics = sessionFactory().getStatistics();
		statistics.clear();

		statistics.naturalIdCacheHit( personNameRole, regionName );
		statistics.naturalIdCacheMiss( personNameRole, regionName );
		statistics.naturalIdCachePut( personNameRole, regionName );

		statistics.entityCacheHit( personRole, regionName );
		statistics.entityCacheMiss( personRole, regionName );
		statistics.entityCachePut( personRole, regionName );

		statistics.collectionCacheHit( personNickNamesRole, regionName );
		statistics.collectionCacheMiss( personNickNamesRole, regionName );
		statistics.collectionCachePut( personNickNamesRole, regionName );

		statistics.getNaturalIdCacheStatistics( cachePrefix + regionName );

		// stats for queries cannot be accessed second level cache regions map
		final String queryString = "select p from Person p";
		final String queryCacheRegionName = "x.y.z";
		final String prefixedQueryCacheRegionName = cachePrefix + '.' + queryCacheRegionName;

		inTransaction(
				// Only way to generate query region (to be accessible via stats) is to execute the query
				session -> session.createQuery( queryString ).setCacheable( true ).setCacheRegion( queryCacheRegionName ).list()
		);

		final SecondLevelCacheStatistics queryCacheStats = statistics.getSecondLevelCacheStatistics( prefixedQueryCacheRegionName );
		assert queryCacheStats != null;

		// note that
		statistics.queryCacheHit( queryString, prefixedQueryCacheRegionName );
		statistics.queryCacheMiss( queryString, prefixedQueryCacheRegionName );
		statistics.queryCachePut( queryString, prefixedQueryCacheRegionName );

//		sessionFactory().getCache().evictQueryRegions();
	}

	@Test
	public void testLegacyCacheSpi() {
		// these need to be the prefixed name
		final String regionName = cachePrefix + '.' + localName;

		final CacheImplementor cache = sessionFactory().getCache();

		// just like stats, the cache for queries cannot be accessed second level cache regions map
		assertEquals( 2, cache.getSecondLevelCacheRegionNames().length );

		boolean foundRegion = false;
		for ( String name : cache.getSecondLevelCacheRegionNames() ) {
			if ( regionName.equals( name ) ) {
				foundRegion = true;
				break;
			}
		}
		if ( !foundRegion ) {
			fail( "Could not find region [" + regionName + "] in reported list of region names" );
		}

		final NavigableRole personEntityName = new NavigableRole( Person.class.getName() );
		final NavigableRole nickNamesRole = personEntityName.append( "nickNames");

		assert cache.getEntityRegionAccess( personEntityName ) != null;
		assert cache.getNaturalIdCacheRegionAccessStrategy( personEntityName ) != null;
		assert cache.getCollectionRegionAccess( nickNamesRole ) != null;
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
