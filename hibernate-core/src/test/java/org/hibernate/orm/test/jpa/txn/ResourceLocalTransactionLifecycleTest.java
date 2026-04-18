/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.txn;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.resource.transaction.spi.TransactionStatus.ACTIVE;
import static org.hibernate.resource.transaction.spi.TransactionStatus.FAILED_COMMIT;
import static org.hibernate.resource.transaction.spi.TransactionStatus.MARKED_ROLLBACK;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.RollbackException;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransactionAccess;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResourceLocalTransactionLifecycleTest {
	@Mock(extraInterfaces = {JdbcSessionOwner.class, JdbcResourceTransactionAccess.class})
	TransactionCoordinatorOwner owner;

	@Mock
	JdbcSessionContext jdbcSessionContext;

	@Mock
	JpaCompliance jpaCompliance;

	@Mock
	JdbcResourceTransaction transaction;

	TransactionCoordinator coordinator;

	@BeforeEach
	void setup() {
		JdbcResourceTransactionAccess jdbcResourceTransactionAccess = (JdbcResourceTransactionAccess) owner;
		JdbcSessionOwner jdbcSessionOwner = (JdbcSessionOwner) owner;
		when( owner.getJdbcSessionOwner() ).thenReturn( jdbcSessionOwner );
		when( jdbcSessionOwner.getJdbcSessionContext() ).thenReturn( jdbcSessionContext );
		when( jdbcSessionContext.getJpaCompliance() ).thenReturn( jpaCompliance );
		when( jdbcResourceTransactionAccess.getResourceLocalTransaction() ).thenReturn( transaction );

		coordinator = new JdbcResourceLocalTransactionCoordinatorBuilderImpl().buildTransactionCoordinator( owner,
				null );
	}

	@Test
	public void testAfterTransactionCompletion_when_rollback_only() {
		when( transaction.getStatus() ).thenReturn( MARKED_ROLLBACK );
		coordinator.getTransactionDriverControl().commit();
		verify( owner ).afterTransactionCompletion( false, false );
		verify( transaction ).rollback();
	}

	@Test
	public void testAfterTransactionCompletion_rollback_exception_on_commit() {
		when( transaction.getStatus() ).thenReturn( ACTIVE );
		doThrow( RollbackException.class ).when( transaction ).commit();
		assertThatThrownBy( () -> coordinator.getTransactionDriverControl().commit() )
				.isExactlyInstanceOf( RollbackException.class );
		verify( transaction ).commit();
		verify( owner ).afterTransactionCompletion( false, false );
	}

	@Test
	public void testAfterTransactionCompletion_on_generic_runtime_exception_on_commit() {
		when( transaction.getStatus() ).thenReturn( ACTIVE );
		doThrow( RuntimeException.class ).when( transaction ).commit();
		assertThatThrownBy( () -> coordinator.getTransactionDriverControl().commit() )
				.isExactlyInstanceOf( RuntimeException.class );
		verify( owner ).afterTransactionCompletion( false, false );
		verify( transaction ).rollback();
	}

	@Test
	@JiraKey(value = "HHH-20336")
	public void testAfterTransactionCompletion_after_failed_commit() {
		when( transaction.getStatus() ).thenReturn( ACTIVE, FAILED_COMMIT );
		doThrow( RuntimeException.class ).when( transaction ).commit();
		assertThatThrownBy( () -> coordinator.getTransactionDriverControl().commit() )
				.isExactlyInstanceOf( RuntimeException.class );
		verify( owner ).afterTransactionCompletion( false, false );
		verify( transaction, never() ).rollback();
	}

	@Test
	public void testAfterTransactionCompletion_on_exception_during_rollback() {
		when( transaction.getStatus() ).thenReturn( ACTIVE );
		doThrow( RuntimeException.class ).when( transaction ).rollback();
		assertThatThrownBy( () -> coordinator.getTransactionDriverControl().rollback() )
				.isExactlyInstanceOf( RuntimeException.class );
		verify( owner ).afterTransactionCompletion( false, false );
		verify( transaction ).rollback();
	}

	@Test
	public void testAfterTransactionCompletion_on_rollback() {
		when( transaction.getStatus() ).thenReturn( ACTIVE );
		coordinator.getTransactionDriverControl().rollback();
		verify( owner ).afterTransactionCompletion( false, false );
		verify( transaction ).rollback();
	}


}
