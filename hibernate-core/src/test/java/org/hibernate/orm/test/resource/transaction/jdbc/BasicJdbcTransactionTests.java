/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jdbc;

import org.hibernate.TransactionException;
import org.hibernate.orm.test.resource.common.SynchronizationCollectorImpl;
import org.hibernate.orm.test.resource.common.SynchronizationErrorImpl;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * @author Steve Ebersole
 */
@SessionFactory
public class BasicJdbcTransactionTests {
	@Test
	public void verifyAssumption(SessionFactoryScope factoryScope) {
		final TransactionCoordinatorBuilder transactionCoordinatorBuilder = factoryScope
				.getSessionFactory()
				.getServiceRegistry()
				.requireService( TransactionCoordinatorBuilder.class );
		assertInstanceOf( JdbcResourceLocalTransactionCoordinatorBuilderImpl.class, transactionCoordinatorBuilder );
	}

	@Test
	public void basicUsageTest(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			final TransactionCoordinator coordinator = session.getTransactionCoordinator();

			final SynchronizationCollectorImpl sync = new SynchronizationCollectorImpl();
			coordinator.getLocalSynchronizations()
					.registerSynchronization( sync );

			coordinator.getTransactionDriverControl().begin();
			assertEquals( 0, sync.getBeforeCompletionCount() );
			assertEquals( 0, sync.getSuccessfulCompletionCount() );
			assertEquals( 0, sync.getFailedCompletionCount() );

			coordinator.getTransactionDriverControl().commit();
			assertEquals( 1, sync.getBeforeCompletionCount() );
			assertEquals( 1, sync.getSuccessfulCompletionCount() );
			assertEquals( 0, sync.getFailedCompletionCount() );
		} );
	}

	@Test
	public void testMarkRollbackOnly(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			final TransactionCoordinator coordinator = session.getTransactionCoordinator();

			assertEquals( TransactionStatus.NOT_ACTIVE,
					coordinator.getTransactionDriverControl().getStatus() );

			session.getTransaction().begin();
			assertEquals( TransactionStatus.ACTIVE, coordinator.getTransactionDriverControl().getStatus() );

			session.getTransaction().markRollbackOnly();
			assertEquals( TransactionStatus.MARKED_ROLLBACK,
					coordinator.getTransactionDriverControl().getStatus() );

			try {
				session.getTransaction().commit();
			}
			catch (TransactionException expected) {
			}
			finally {
				assertThat(
						coordinator.getTransactionDriverControl().getStatus(),
						anyOf(
								is( TransactionStatus.NOT_ACTIVE ),
								is( TransactionStatus.ROLLED_BACK )
						)
				);
			}
		} );
	}

	@Test
	@SuppressWarnings("EmptyCatchBlock")
	public void testSynchronizationFailure(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
			final TransactionCoordinator coordinator = session.getTransactionCoordinator();

			assertEquals( TransactionStatus.NOT_ACTIVE,
					coordinator.getTransactionDriverControl().getStatus() );
			coordinator.getLocalSynchronizations().registerSynchronization( SynchronizationErrorImpl.forBefore() );

			coordinator.getTransactionDriverControl().begin();
			assertEquals( TransactionStatus.ACTIVE, coordinator.getTransactionDriverControl().getStatus() );

			try {
				coordinator.getTransactionDriverControl().commit();
			}
			catch (Exception expected) {
			}
			finally {
				assertThat(
						coordinator.getTransactionDriverControl().getStatus(),
						anyOf(
								is( TransactionStatus.NOT_ACTIVE ),
								is( TransactionStatus.ROLLED_BACK )
						)
				);
			}
		} );
	}
}
