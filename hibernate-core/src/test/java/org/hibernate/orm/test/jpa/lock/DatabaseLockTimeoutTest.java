/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.lock.internal.PessimisticEntityLockException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Jpa(annotatedClasses = Lockable.class)
@RequiresDialect(H2Dialect.class)
class DatabaseLockTimeoutTest {

	@Test
	void testDatabaseDefaultLockTimeout(EntityManagerFactoryScope scope) {
		final var locked = new Lockable( "locked" );
		final var other = new Lockable( "other" );
		scope.inTransaction( em -> {
			em.persist( locked );
			em.persist( other );
		} );
		try {
			scope.inEntityManager( em -> {
				final var transaction = em.getTransaction();
				transaction.begin();
				try {
					final var managed = em.find( Lockable.class, locked.getId() );
					scope.inTransaction( holder -> {
						holder.find( Lockable.class, locked.getId(), LockModeType.PESSIMISTIC_WRITE );
						// Use H2's configured database timeout without supplying a JPA timeout hint.
						final var exception = assertThrows( LockTimeoutException.class,
								() -> em.lock( managed, LockModeType.PESSIMISTIC_WRITE ) );
						assertSame( managed, exception.getObject() );
						assertInstanceOf( PessimisticEntityLockException.class, exception.getCause() );
						assertFalse( transaction.getRollbackOnly() );
					} );
					em.find( Lockable.class, other.getId() ).setName( "updated after timeout" );
					transaction.commit();
				}
				finally {
					if ( transaction.isActive() ) {
						transaction.rollback();
					}
				}
			} );
			scope.inTransaction( em -> assertEquals(
					"updated after timeout", em.find( Lockable.class, other.getId() ).getName() ) );
		}
		finally {
			scope.dropData();
		}
	}
}
