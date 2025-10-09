/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.mutable.cached;

import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests of mutable natural ids stored in second level cache
 *
 * @author Guenther Demetz
 * @author Steve Ebersole
 */
public abstract class CachedMutableNaturalIdTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@Test
	public void testNaturalIdChangedWhileAttached(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.persist( new Another( "it" ) )
		);

		scope.inTransaction(
				(session) -> {
					Another it = session.bySimpleNaturalId( Another.class ).load( "it" );
					assertNotNull( it );
					// change it's name
					it.setName( "it2" );
				}
		);

		scope.inTransaction(
				(session) -> {
					final Another shouldBeGone = session.bySimpleNaturalId( Another.class ).load( "it" );
					assertNull( shouldBeGone );
					final Another updated = session.bySimpleNaturalId( Another.class ).load( "it2" );
					assertNotNull( updated );
				}
		);
	}

	@Test
	public void testNaturalIdChangedWhileDetached(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.persist( new Another( "it" ) )
		);

		final Another detached = scope.fromTransaction(
				(session) -> {
					final Another it = session.bySimpleNaturalId( Another.class ).load( "it" );
					assertNotNull( it );
					return it;
				}
		);

		detached.setName( "it2" );

		scope.inTransaction(
				(session) -> session.merge( detached )
		);

		scope.inTransaction(
				(session) -> {
					final Another shouldBeGone = session.bySimpleNaturalId( Another.class ).load( "it" );
					assertNull( shouldBeGone );
					final Another updated = session.bySimpleNaturalId( Another.class ).load( "it2" );
					assertNotNull( updated );
				}
		);
	}

	@Test
	public void testNaturalIdReCachingWhenNeeded(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		final Integer id = scope.fromTransaction(
				(session) -> {
					Another it = new Another( "it" );
					session.persist( it );
					return it.getId();
				}
		);

		scope.inTransaction(
				(session) -> {
					final Another it = session.find( Another.class, id );
					it.setName( "it2" );
					// changing something but not the natural-id's
					it.setSurname( "surname" );

				}
		);

		scope.inTransaction(
				(session) -> {
					final Another shouldBeGone = session.bySimpleNaturalId(Another.class).load("it");
					assertNull( shouldBeGone );
					assertEquals( 0, statistics.getNaturalIdCacheHitCount() );
				}
		);

		// finally there should be only 2 NaturalIdCache puts : 1. insertion, 2. when updating natural-id from 'it' to 'it2'
		assertEquals( 2, statistics.getNaturalIdCachePutCount() );
	}

	@Test
	@JiraKey( "HHH-7245" )
	public void testNaturalIdChangeAfterResolveEntityFrom2LCache(SessionFactoryScope scope) {

		final Integer id = scope.fromTransaction(
				(session) -> {
					AllCached it = new AllCached( "it" );
					session.persist( it );
					return it.getId();
				}
		);

		scope.inTransaction(
				(session) -> {
					final AllCached it = session.find( AllCached.class, id );
					it.setName( "it2" );

					final AllCached shouldBeGone = session.bySimpleNaturalId( AllCached.class ).load( "it" );
					assertNull( shouldBeGone );

					final AllCached updated = session.bySimpleNaturalId( AllCached.class ).load( "it2" );
					assertNotNull( updated );
				}
		);
	}

	@Test
	@JiraKey( "HHH-12657" )
	public void testBySimpleNaturalIdResolveEntityFrom2LCacheSubClass(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.persist( new SubClass( "it" ) )
		);

		scope.inTransaction(
				(session) -> {
					// load by super-type
					final AllCached bySuper = session.bySimpleNaturalId( AllCached.class ).load( "it" );
					assertNotNull( bySuper );

					// load by concrete type
					final SubClass byConcrete = session.bySimpleNaturalId( SubClass.class ).load( "it" );
					assertNotNull( byConcrete );
				}
		);
	}

	@Test
	@JiraKey( "HHH-16557" )
	public void testCreateDeleteRecreate(SessionFactoryScope scope) {

		final Integer id = scope.fromTransaction(
				(session) -> {
					AllCached it = new AllCached( "it" );
					session.persist(it);
					session.remove(it);
					// insert-remove might happen in an app driven by users GUI interactions
					return it.getId();
				}
		);

		// now recreate with same naturalId value
		scope.inTransaction(
				(session) -> {
					AllCached it = new AllCached( "it" );
					session.persist(it);
					// resolving from first level cache
					assertNotNull(session.bySimpleNaturalId( AllCached.class ).load( "it" ));
				}
		);

		scope.inTransaction(
				(session) -> {
					// should resolve from second level cache
					final AllCached shouldBeThere = session.bySimpleNaturalId( AllCached.class ).load( "it" );
					assertNotNull( shouldBeThere );
					assert(id.compareTo(shouldBeThere.getId()) != 0);
				}
		);
	}

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
			assertEquals(4, sfi.getStatistics().getNaturalIdCacheHitCount(), "we expected now 4 hits");
			assertNotNull(person);
			session.remove(person); // evicts natural-id from first & second level cache
			person = session.bySimpleNaturalId(AllCached.class).load("John Doe");
			assertEquals(4, sfi.getStatistics().getNaturalIdCacheHitCount()); // thus hits should not increment
		});
	}

}
