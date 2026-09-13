/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import jakarta.persistence.LockModeType;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.Session;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hibernate.jpa.HibernateHints.HINT_FOLLOW_ON_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = { Lockable.class, UnversionedLock.class })
class QueryLockingManagedEntityTest {

	private Lockable first;
	private Lockable second;

	@BeforeEach
	void createTestData(EntityManagerFactoryScope scope) {
		first = new Lockable( "first" );
		second = new Lockable( "second" );
		scope.inTransaction( em -> {
			em.persist( first );
			em.persist( second );
		} );
	}

	@AfterEach
	void dropTestData(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@EnumSource(LockModeType.class)
	void testManagedAndNewQueryResults(LockModeType mode, EntityManagerFactoryScope scope) {
		testManagedAndNewQueryResults( mode, Locking.FollowOn.ALLOW, scope );
	}

	@ParameterizedTest
	@EnumSource(value = LockModeType.class, names = {
			"PESSIMISTIC_READ", "PESSIMISTIC_WRITE", "PESSIMISTIC_FORCE_INCREMENT"
	})
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
	void testManagedAndNewQueryResultsWithFollowOnLocking(LockModeType mode, EntityManagerFactoryScope scope) {
		testManagedAndNewQueryResults( mode, Locking.FollowOn.FORCE, scope );
	}

	private void testManagedAndNewQueryResults(
			LockModeType mode, Locking.FollowOn followOn, EntityManagerFactoryScope scope) {
		// Use different starting versions so that each result must be checked against its own version.
		second = scope.fromTransaction( em -> {
			final var entity = em.find( Lockable.class, second.getId() );
			entity.setName( "updated second" );
			return entity;
		} );
		assertNotEquals( first.getVersion(), second.getVersion() );
		scope.inTransaction( em -> {
			final var managed = em.find( Lockable.class, first.getId() );
			// Each entity appears in multiple rows, and only the first is already managed.
			final var query = em.createQuery(
					"select l from Lockable l cross join Lockable other order by l.id", Lockable.class )
					.setHint( HINT_FOLLOW_ON_STRATEGY, followOn )
					.setLockMode( mode );
			for ( int i = 0; i < 2; i++ ) {
				final var results = query.getResultList();
				assertEquals( 2, results.stream().distinct().count() );
				assertSame( managed, results.get( 0 ) );
				final int increment = mode == LockModeType.PESSIMISTIC_FORCE_INCREMENT ? 1 : 0;
				for ( var result : results ) {
					final var initial = result.getId().equals( first.getId() ) ? first : second;
					assertEquals( initial.getVersion() + increment, result.getVersion() );
					if ( LockMode.fromJpaLockMode( mode ).isPessimistic() ) {
						assertEquals( mode, em.getLockMode( result ) );
					}
				}
			}
		} );
		final int increment = switch ( mode ) {
			case WRITE, OPTIMISTIC_FORCE_INCREMENT, PESSIMISTIC_FORCE_INCREMENT -> 1;
			default -> 0;
		};
		scope.inTransaction( em -> {
			assertEquals( first.getVersion() + increment, em.find( Lockable.class, first.getId() ).getVersion() );
			assertEquals( second.getVersion() + increment, em.find( Lockable.class, second.getId() ).getVersion() );
		} );
	}

	@ParameterizedTest
	@EnumSource(value = LockModeType.class, names = { "PESSIMISTIC_READ", "PESSIMISTIC_WRITE" })
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
	void testFollowOnLockingPreservesManagedState(LockModeType mode, EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final var managed = em.find( Lockable.class, first.getId() );
			managed.setName( "local change" );
			assertSame( managed, em.createQuery( "from Lockable where id = :id", Lockable.class )
					.setParameter( "id", first.getId() )
					.setFlushMode( FlushModeType.COMMIT )
					.setHint( HINT_FOLLOW_ON_STRATEGY, Locking.FollowOn.FORCE )
					.setLockMode( mode )
					.getSingleResult() );
			assertEquals( mode, em.getLockMode( managed ) );
			assertEquals( "local change", managed.getName() );
		} );
		scope.inTransaction( em -> assertEquals(
				"local change", em.find( Lockable.class, first.getId() ).getName() ) );
	}

	@ParameterizedTest
	@EnumSource(value = LockModeType.class, names = { "READ", "OPTIMISTIC", "WRITE", "OPTIMISTIC_FORCE_INCREMENT" })
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	void testConcurrentUpdateAfterQuery(LockModeType mode, EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var transaction = em.getTransaction();
			transaction.begin();
			try {
				final var managed = em.find( Lockable.class, first.getId() );
				assertSame( managed, em.createQuery( "from Lockable where id = :id", Lockable.class )
						.setParameter( "id", first.getId() )
						.setLockMode( mode )
						.getSingleResult() );
				scope.inTransaction( other -> other.find( Lockable.class, first.getId() ).setName( "changed" ) );
				final var exception = assertThrows( RollbackException.class, transaction::commit );
				assertInstanceOf( OptimisticLockException.class, exception.getCause() );
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		} );
	}

	static Stream<Arguments> pessimisticQueriesFromNone() {
		return Stream.of( LockModeType.PESSIMISTIC_READ, LockModeType.PESSIMISTIC_WRITE )
				.flatMap( mode -> Stream.of(
						Arguments.of( mode, false, false ),
						Arguments.of( mode, false, true ),
						Arguments.of( mode, true, false ),
						Arguments.of( mode, true, true )
				) );
	}

	@ParameterizedTest(name = "{0}, loadInTransaction={1}, concurrentUpdate={2}")
	@MethodSource("pessimisticQueriesFromNone")
	void testPessimisticQueryFromNone(
			LockModeType mode, boolean loadInTransaction, boolean concurrentUpdate,
			EntityManagerFactoryScope scope) {
		testPessimisticQueryFromNone( mode, loadInTransaction, concurrentUpdate, Locking.FollowOn.ALLOW, scope );
	}

	@ParameterizedTest(name = "{0}, loadInTransaction={1}, concurrentUpdate={2}")
	@MethodSource("pessimisticQueriesFromNone")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
	void testPessimisticFollowOnQueryFromNone(
			LockModeType mode, boolean loadInTransaction, boolean concurrentUpdate,
			EntityManagerFactoryScope scope) {
		testPessimisticQueryFromNone( mode, loadInTransaction, concurrentUpdate, Locking.FollowOn.FORCE, scope );
	}

	private void testPessimisticQueryFromNone(
			LockModeType mode, boolean loadInTransaction, boolean concurrentUpdate,
			Locking.FollowOn followOn, EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final var transaction = em.getTransaction();
			try {
				if ( loadInTransaction ) {
					transaction.begin();
				}
				final var managed = em.find( Lockable.class, first.getId() );
				if ( loadInTransaction ) {
					transaction.commit();
				}
				// Both a load outside a transaction and a completed transaction leave the entry at NONE.
				assertEquals( LockMode.NONE, em.unwrap( Session.class ).getCurrentLockMode( managed ) );
				if ( concurrentUpdate ) {
					scope.inTransaction( writer -> {
						final var updated = writer.find( Lockable.class, first.getId() );
						updated.setName( "changed" );
						writer.flush();
						assertEquals( managed.getVersion() + 1, updated.getVersion() );
					} );
				}
				transaction.begin();
				assertEquals( LockMode.NONE, em.unwrap( Session.class ).getCurrentLockMode( managed ) );
				final var query = em.createQuery( "from Lockable where id = :id", Lockable.class )
						.setParameter( "id", first.getId() )
						.setHint( HINT_FOLLOW_ON_STRATEGY, followOn )
						.setLockMode( mode );
				if ( concurrentUpdate ) {
					assertThrows( OptimisticLockException.class, query::getSingleResult );
					assertTrue( transaction.getRollbackOnly() );
				}
				else {
					assertSame( managed, query.getSingleResult() );
					assertEquals( mode, em.getLockMode( managed ) );
					assertFalse( transaction.getRollbackOnly() );
					transaction.commit();
				}
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		} );
	}

	@ParameterizedTest
	@EnumSource(value = LockModeType.class, names = {
			"READ", "OPTIMISTIC", "WRITE", "OPTIMISTIC_FORCE_INCREMENT", "PESSIMISTIC_FORCE_INCREMENT"
	})
	void testUnversionedManagedQueryResult(LockModeType mode, EntityManagerFactoryScope scope) {
		final var unversioned = new UnversionedLock();
		scope.inTransaction( em -> em.persist( unversioned ) );
		scope.inEntityManager( em -> {
			final var transaction = em.getTransaction();
			transaction.begin();
			try {
				em.find( UnversionedLock.class, unversioned.getId() );
				final var query = em.createQuery( "from UnversionedLock", UnversionedLock.class ).setLockMode( mode );
				assertThrows( PersistenceException.class, query::getSingleResult );
			}
			finally {
				transaction.rollback();
			}
		} );
	}
}
