/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.naturalid.mutable.cached;

import org.hibernate.LockOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Tests of mutable natural ids stored in second level cache
 * 
 * @author Guenther Demetz
 * @author Steve Ebersole
 */
public abstract class CachedMutableNaturalIdTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.createQuery( "delete from Another" ).executeUpdate();
					session.createQuery( "delete from AllCached" ).executeUpdate();
					session.createQuery( "delete from SubClass" ).executeUpdate();
					session.createQuery( "delete from A" ).executeUpdate();
				}
		);

		final CacheImplementor cache = scope.getSessionFactory().getCache();
		cache.evictAllRegions();
	}

	@Test
	public void testNaturalIdChangedWhileAttached(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.save( new Another( "it" ) )
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
				(session) -> session.save( new Another( "it" ) )
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
				(session) -> session.update( detached )
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
					session.save( it );
					return it.getId();
				}
		);

		scope.inTransaction(
				(session) -> {
					final Another it = session.byId( Another.class ).load( id );
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
	@TestForIssue( jiraKey = "HHH-7245" )
	public void testNaturalIdChangeAfterResolveEntityFrom2LCache(SessionFactoryScope scope) {

		final Integer id = scope.fromTransaction(
				(session) -> {
					AllCached it = new AllCached( "it" );
					session.save( it );
					return it.getId();
				}
		);

		scope.inTransaction(
				(session) -> {
					final AllCached it = session.byId( AllCached.class ).load( id );
					it.setName( "it2" );

					final AllCached shouldBeGone = session.bySimpleNaturalId( AllCached.class ).load( "it" );
					assertNull( shouldBeGone );

					final AllCached updated = session.bySimpleNaturalId( AllCached.class ).load( "it2" );
					assertNotNull( updated );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12657" )
	public void testBySimpleNaturalIdResolveEntityFrom2LCacheSubClass(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.save( new SubClass( "it" ) )
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
	@TestForIssue( jiraKey = "HHH-16557" )
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
			assertEquals("we expected now 4 hits", 4, sfi.getStatistics().getNaturalIdCacheHitCount());
			assertNotNull(person);
			session.remove(person); // evicts natural-id from first & second level cache
			person = session.bySimpleNaturalId(AllCached.class).load("John Doe");
			assertEquals(4, sfi.getStatistics().getNaturalIdCacheHitCount()); // thus hits should not increment
		});
	}
	
	@Test
	public void testReattachUnmodifiedInstance(SessionFactoryScope scope) {
		final B created = scope.fromTransaction(
				(session) -> {
					A a = new A();
					B b = new B();
					b.naturalid = 100;
					session.persist( a );
					session.persist( b );
					b.assA = a;
					a.assB.add( b );

					return b;
				}
		);

		scope.inTransaction(
				(session) -> {
					// HHH-7513 failure during reattachment
					session.buildLockRequest( LockOptions.NONE ).lock( created );
					session.delete( created.assA );
					session.delete( created );
				}
		);

		scope.inTransaction(
				(session) -> {
					// true if the re-attachment worked
					assertEquals( session.createQuery( "FROM A" ).list().size(), 0 );
					assertEquals( session.createQuery( "FROM B" ).list().size(), 0 );
				}
		);
	}

}

