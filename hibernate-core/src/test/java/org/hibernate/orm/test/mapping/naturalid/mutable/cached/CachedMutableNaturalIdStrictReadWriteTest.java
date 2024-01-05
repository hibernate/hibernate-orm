/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.mutable.cached;

import org.hibernate.Transaction;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.stat.NaturalIdStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.*;
import static org.hibernate.testing.cache.CachingRegionFactory.DEFAULT_ACCESSTYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@ServiceRegistry(
		settings = {
				@Setting( name = USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = DEFAULT_ACCESSTYPE, value = "nonstrict-read-write" ),
				@Setting(name = DISABLE_NATURAL_ID_RESOLUTIONS_CACHE, value = "false"),
				@Setting( name = GENERATE_STATISTICS, value = "true" )
		}
)
@DomainModel( annotatedClasses = {A.class, Another.class, AllCached.class, B.class, SubClass.class} )
@SessionFactory
public class CachedMutableNaturalIdStrictReadWriteTest extends CachedMutableNaturalIdStrictTest {
	@Test
	@JiraKey("HHH-16558")
	public void testCacheVerifyHits(SessionFactoryScope scope) {
		scope.inTransaction((session) -> {
			AllCached aAllCached = new AllCached();
			aAllCached.setName("John Doe");
			session.persist(aAllCached);
			SessionFactoryImpl sfi = (SessionFactoryImpl) session.getSessionFactory();
			sfi.getStatistics().clear();
		});

		scope.inTransaction((session) -> {
			System.out.println("Native load by natural-id, generate first hit");
			SessionFactoryImpl sfi = (SessionFactoryImpl) session.getSessionFactory();
			AllCached person = session.bySimpleNaturalId(AllCached.class).load("John Doe");
			assertNotNull(person);
			System.out.println("NaturalIdCacheHitCount: " + sfi.getStatistics().getNaturalIdCacheHitCount());
			System.out.println("SecondLevelCacheHitCount: " + sfi.getStatistics().getSecondLevelCacheHitCount());
			assertEquals(1, sfi.getStatistics().getNaturalIdCacheHitCount());
			assertEquals(1, sfi.getStatistics().getSecondLevelCacheHitCount());
		});

		scope.inTransaction((session) -> {
			System.out.println("Native load by natural-id, generate second hit");

			SessionFactoryImpl sfi = (SessionFactoryImpl) session.getSessionFactory();
			//tag::caching-entity-natural-id-example[]
			AllCached person = session.bySimpleNaturalId(AllCached.class).load("John Doe");
			assertNotNull(person);

			// resolve in persistence context (first level cache)
			session.bySimpleNaturalId(AllCached.class).load("John Doe");
			System.out.println("NaturalIdCacheHitCount: " + sfi.getStatistics().getNaturalIdCacheHitCount());
			System.out.println("SecondLevelCacheHitCount: " + sfi.getStatistics().getSecondLevelCacheHitCount());
			assertEquals(2, sfi.getStatistics().getNaturalIdCacheHitCount());
			assertEquals(2, sfi.getStatistics().getSecondLevelCacheHitCount());

			session.clear();
			//  persistence context (first level cache) empty, should resolve from second level cache
			System.out.println("Native load by natural-id, generate third hit");
			person = session.bySimpleNaturalId(AllCached.class).load("John Doe");
			System.out.println("NaturalIdCacheHitCount: " + sfi.getStatistics().getNaturalIdCacheHitCount());
			System.out.println("SecondLevelCacheHitCount: " + sfi.getStatistics().getSecondLevelCacheHitCount());
			assertNotNull(person);
			assertEquals(3, sfi.getStatistics().getNaturalIdCacheHitCount());
			assertEquals(3, sfi.getStatistics().getSecondLevelCacheHitCount());

			//Remove the entity from the persistence context
			Integer id = person.getId();

			session.detach(person); // still it should resolve from second level cache after this

			System.out.println("Native load by natural-id, generate 4. hit");
			person = session.bySimpleNaturalId(AllCached.class).load("John Doe");
			System.out.println("NaturalIdCacheHitCount: " + sfi.getStatistics().getNaturalIdCacheHitCount());
			assertEquals("we expected now 4 hits", 4, sfi.getStatistics().getNaturalIdCacheHitCount());
			assertNotNull(person);
			session.remove(person); // evicts natural-id from first & second level cache
			person = session.bySimpleNaturalId(AllCached.class).load("John Doe");
			assertEquals(4, sfi.getStatistics().getNaturalIdCacheHitCount()); // thus hits should not increment
		});
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9200" )
	public void testNaturalIdCacheStatisticsReset(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> {
					session.save( new Another( "IT" ) );
				}
		);

		NaturalIdStatistics stats = statistics.getNaturalIdStatistics( Another.class.getName() );
		assertEquals( 1, stats.getCachePutCount() );

		statistics.clear();

		// Refresh statistics reference.
		stats = statistics.getNaturalIdStatistics( Another.class.getName() );
		assertEquals( 0, stats.getCachePutCount() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7278" )
	public void testInsertedNaturalIdCachedAfterTransactionSuccess(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> session.save( new Another( "it" ) )
		);

		scope.inTransaction(
				(session) -> {
					final Another it = session.bySimpleNaturalId( Another.class ).load( "it" );
					assertNotNull( it );
				}
		);
		assertEquals( 1, statistics.getNaturalIdCacheHitCount() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9203" )
	public void testToMapConversion(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> session.save( new AllCached( "IT" ) )
		);

		final NaturalIdStatistics naturalIdStatistics = statistics.getNaturalIdStatistics( AllCached.class.getName() );
		assertEquals( 1, naturalIdStatistics.getCachePutCount() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7278" )
	public void testChangedNaturalIdCachedAfterTransactionSuccess(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> session.save( new Another( "it" ) )
		);

		scope.inTransaction(
				(session) -> {
					final Another it = session.bySimpleNaturalId( Another.class ).load( "it" );
					assertNotNull( it );

					it.setName( "modified" );
				}
		);

		statistics.clear();

		scope.inTransaction(
				(session) -> {
					final Another it = session.bySimpleNaturalId( Another.class ).load( "modified" );
					assertNotNull( it );
				}
		);

		assertEquals( 1, statistics.getNaturalIdCacheHitCount() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7309" )
	@Override
	public void testInsertUpdateEntity_NaturalIdCachedAfterTransactionSuccess(SessionFactoryScope scope) {
	 	super.testInsertUpdateEntity_NaturalIdCachedAfterTransactionSuccess(scope);
		assertEquals( "In a strict access strategy we would expect a hit here", 1, scope.getSessionFactory().getStatistics().getNaturalIdCacheHitCount() );

	}


}
