/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test that we can upgrade locks
 *
 * @author Scott Marlow
 */
@Jpa(annotatedClasses = Lock.class)
public class UpgradeLockTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	public void testUpgradeReadLockToOptimisticForceIncrement(
			boolean concurrentUpdate, EntityManagerFactoryScope scope) {
		final var lock = new Lock( "name" );
		scope.inTransaction( em -> em.persist( lock ) );
		scope.inEntityManager( em -> {
			final var transaction = em.getTransaction();
			transaction.begin();
			try {
				final var managed = em.find( Lock.class, lock.getId() );
				em.lock( managed, LockModeType.READ );
				em.lock( managed, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
				assertEquals( LockModeType.OPTIMISTIC_FORCE_INCREMENT, em.getLockMode( managed ) );
				// Leave the entity unchanged so an ordinary update cannot mask a missing lock upgrade.
				if ( concurrentUpdate ) {
					scope.inTransaction( writer -> writer.find( Lock.class, lock.getId() ).setName( "renamed" ) );
					final var failure = assertThrows( RollbackException.class, transaction::commit );
					assertInstanceOf( OptimisticLockException.class, failure.getCause() );
				}
				else {
					transaction.commit();
				}
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		} );
		scope.inTransaction( em -> {
			final var result = em.find( Lock.class, lock.getId() );
			assertEquals( lock.getVersion() + 1, result.getVersion() );
			assertEquals( concurrentUpdate ? "renamed" : "name", result.getName() );
		} );
	}
}
