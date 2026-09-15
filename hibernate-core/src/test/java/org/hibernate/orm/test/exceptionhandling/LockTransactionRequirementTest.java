/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ParameterizedClass
@EnumSource(BaseJpaOrNativeBootstrapFunctionalTestCase.BootstrapMethod.class)
class LockTransactionRequirementTest extends BaseJpaOrNativeBootstrapFunctionalTestCase {

	private final boolean jpaBootstrap;

	LockTransactionRequirementTest(BootstrapMethod bootstrapMethod) {
		super( bootstrapMethod );
		jpaBootstrap = bootstrapMethod == BootstrapMethod.JPA;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Lockable.class };
	}

	@Test
	void testLockNoneWithoutTransaction() {
		persistEntity();
		try ( var session = sessionFactory().openSession() ) {
			final var entity = session.find( Lockable.class, 1 );
			assertAll(
					() -> checkWithoutTransaction( () -> session.lock( entity, LockModeType.NONE ) ),
					() -> checkWithoutTransaction( () -> session.lock( entity, LockModeType.NONE, Map.of() ) ),
					() -> checkWithoutTransaction( () -> session.lock( entity, LockModeType.NONE, new LockOption[0] ) )
			);
		}
	}

	@Test
	void testLockNoneWithTransaction() {
		persistEntity();
		sessionFactory().inTransaction( session -> {
			final var entity = session.find( Lockable.class, 1 );
			session.lock( entity, LockModeType.NONE );
			session.lock( entity, LockModeType.NONE, Map.of() );
			session.lock( entity, LockModeType.NONE, new LockOption[0] );
		} );
	}

	@Test
	void testStatelessRefreshWithLockWithoutTransaction() {
		persistEntity();
		try ( var session = sessionFactory().openStatelessSession() ) {
			final var entity = new Lockable();
			if ( !jpaBootstrap && !supportsStatelessOptimistic() ) {
				assertThrows( org.hibernate.HibernateException.class, () -> session.refresh( entity, LockModeType.OPTIMISTIC ) );
				return;
			}
			checkWithoutTransaction( () -> session.refresh( entity, LockModeType.OPTIMISTIC ) );
			if ( !jpaBootstrap ) {
				assertEquals( "original", entity.name );
			}
		}
	}

	@Test
	void testStatelessRefreshWithLockWithTransaction() {
		persistEntity();
		if ( !supportsStatelessOptimistic() ) {
			try ( var session = sessionFactory().openStatelessSession() ) {
				final var transaction = session.beginTransaction();
				try {
					assertThrows( org.hibernate.HibernateException.class,
							() -> session.refresh( new Lockable(), LockModeType.OPTIMISTIC ) );
				}
				finally {
					transaction.rollback();
				}
			}
			return;
		}
		sessionFactory().inStatelessTransaction( session -> {
			final var entity = new Lockable();
			session.refresh( entity, LockModeType.OPTIMISTIC );
			assertEquals( "original", entity.name );
		} );
	}

	@Test
	void testStatelessRefreshNoneWithoutTransaction() {
		persistEntity();
		try ( var session = sessionFactory().openStatelessSession() ) {
			final var entity = new Lockable();
			session.refresh( entity, LockModeType.NONE );
			assertEquals( "original", entity.name );
		}
	}

	@Test
	void testFindAndRefreshNoneWithoutTransaction() {
		persistEntity();
		try ( var session = sessionFactory().openSession() ) {
			final var entity = session.find( Lockable.class, 1, LockModeType.NONE );
			entity.name = "changed";
			session.refresh( entity, LockModeType.NONE );
			assertEquals( "original", entity.name );
		}
	}

	private boolean supportsStatelessOptimistic() {
		final var concurrency = sessionFactory().getJdbcServices().getJdbcEnvironment().getTransactionConcurrency();
		return java.util.stream.Stream.of(
				org.hibernate.dialect.lock.spi.Operation.READ,
				org.hibernate.dialect.lock.spi.Operation.SHARED_LOCK_READ,
				org.hibernate.dialect.lock.spi.Operation.UPDATE_LOCK_READ )
				.filter( concurrency::supports ).map( concurrency::getReadGuarantees )
				.anyMatch( g -> g.preventsDirtyReads() && g.preventsConcurrentModification()
						&& g.holdsRowLockUntilTransactionCompletion() );
	}

	private void checkWithoutTransaction(Runnable action) {
		if ( jpaBootstrap ) {
			assertThrows( TransactionRequiredException.class, action::run );
		}
		else {
			action.run();
		}
	}

	private void persistEntity() {
		sessionFactory().inTransaction( session -> {
			final var entity = new Lockable();
			entity.name = "original";
			session.persist( entity );
		} );
	}

	@Entity(name = "TransactionLockable")
	static class Lockable {
		@Id
		int id = 1;
		@Version
		int version;
		String name;
	}
}
