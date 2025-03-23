/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.mutable.cached;

import org.hibernate.Transaction;
import org.hibernate.stat.NaturalIdStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.GENERATE_STATISTICS;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.testing.cache.CachingRegionFactory.DEFAULT_ACCESSTYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@ServiceRegistry(
		settings = {
				@Setting( name = USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = DEFAULT_ACCESSTYPE, value = "nonstrict-read-write" ),
				@Setting( name = GENERATE_STATISTICS, value = "true" )
		}
)
@DomainModel( annotatedClasses = {A.class, Another.class, AllCached.class, B.class, SubClass.class} )
@SessionFactory
public class CachedMutableNaturalIdStrictReadWriteTest extends CachedMutableNaturalIdTest {

	@Test
	@JiraKey( value = "HHH-9203" )
	public void testToMapConversion(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> session.persist( new AllCached( "IT" ) )
		);

		final NaturalIdStatistics naturalIdStatistics = statistics.getNaturalIdStatistics( AllCached.class.getName() );
		assertEquals( 1, naturalIdStatistics.getCachePutCount() );
	}

	@Test
	@JiraKey( value = "HHH-7278" )
	public void testInsertedNaturalIdCachedAfterTransactionSuccess(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> session.persist( new Another( "it" ) )
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
	@JiraKey( value = "HHH-7278" )
	public void testInsertedNaturalIdNotCachedAfterTransactionFailure(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inSession(
				(session) -> {
					final Transaction transaction = session.getTransaction();
					transaction.begin();

					session.persist( new Another( "it" ) );
					session.flush();

					transaction.rollback();
				}
		);

		scope.inTransaction(
				(session) -> {
					final Another it = session.bySimpleNaturalId( Another.class ).load( "it" );
					assertNull( it );
					assertEquals( 0, statistics.getNaturalIdCacheHitCount() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-7278" )
	public void testChangedNaturalIdCachedAfterTransactionSuccess(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> session.persist( new Another( "it" ) )
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
	@JiraKey( value = "HHH-7278" )
	public void testChangedNaturalIdNotCachedAfterTransactionFailure(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> session.persist( new Another( "it" ) )
		);

		scope.inTransaction(
				(session) -> {
					final Another it = session.bySimpleNaturalId( Another.class ).load( "it" );
					assertNotNull( it );

					it.setName( "modified" );
					session.flush();
					session.getTransaction().markRollbackOnly();
				}
		);

		statistics.clear();

		scope.inTransaction(
				(session) -> {
					final Another modified = session.bySimpleNaturalId( Another.class ).load( "modified" );
					assertNull( modified );

					final Another original = session.bySimpleNaturalId( Another.class ).load( "it" );
					assertNotNull( original );
				}
		);

		assertEquals(0, statistics.getNaturalIdCacheHitCount());
	}

	@Test
	@JiraKey( value = "HHH-7309" )
	public void testInsertUpdateEntity_NaturalIdCachedAfterTransactionSuccess(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> {
					Another it = new Another( "it" );
					// schedules an InsertAction
					session.persist( it );

					// schedules an UpdateAction
					// 	- without bug-fix this will re-cache natural-id with identical key and at same time invalidate it
					it.setSurname( "1234" );
				}
		);

		scope.inTransaction(
				(session) -> {
					final Another it = session.bySimpleNaturalId( Another.class ).load( "it" );
					assertNotNull( it );
				}
		);

		assertEquals( "In a strict access strategy we would expect a hit here", 1, statistics.getNaturalIdCacheHitCount() );
	}

	@Test
	@JiraKey( value = "HHH-9200" )
	public void testNaturalIdCacheStatisticsReset(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction(
				(session) -> {
					session.persist( new Another( "IT" ) );
				}
		);

		NaturalIdStatistics stats = statistics.getNaturalIdStatistics( Another.class.getName() );
		assertEquals( 1, stats.getCachePutCount() );

		statistics.clear();

		// Refresh statistics reference.
		stats = statistics.getNaturalIdStatistics( Another.class.getName() );
		assertEquals( 0, stats.getCachePutCount() );
	}
}
