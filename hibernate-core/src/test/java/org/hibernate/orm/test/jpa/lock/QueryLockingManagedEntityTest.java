/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
		scope.inTransaction( em -> {
			final var managed = em.find( Lockable.class, first.getId() );
			// Each entity appears in multiple rows, and only the first is already managed.
			final var query = em.createQuery(
					"select l from Lockable l cross join Lockable other order by l.id", Lockable.class )
					.setLockMode( mode );
			for ( int i = 0; i < 2; i++ ) {
				final var results = query.getResultList();
				assertEquals( 2, results.stream().distinct().count() );
				assertSame( managed, results.get( 0 ) );
				final int increment = mode == LockModeType.PESSIMISTIC_FORCE_INCREMENT ? 1 : 0;
				for ( var result : results ) {
					assertEquals( first.getVersion() + increment, result.getVersion() );
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
				assertThrows( RollbackException.class, transaction::commit );
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
