/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.resource.transaction.jdbc;

import org.hibernate.TransactionException;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.test.resource.common.SynchronizationCollectorImpl;
import org.hibernate.test.resource.common.SynchronizationErrorImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class BasicJdbcTransactionTests {

	@Test
	public void basicUsageTest() throws Exception {
		final TransactionCoordinatorOwnerTestingImpl owner = new TransactionCoordinatorOwnerTestingImpl();
		final JdbcResourceLocalTransactionCoordinatorBuilderImpl transactionCoordinatorBuilder =
				new JdbcResourceLocalTransactionCoordinatorBuilderImpl();

		final TransactionCoordinator transactionCoordinator = transactionCoordinatorBuilder.buildTransactionCoordinator(
				owner,
				new TransactionCoordinatorBuilder.Options() {
					@Override
					public boolean shouldAutoJoinTransaction() {
						return false;
					}
				}
		);

		SynchronizationCollectorImpl sync = new SynchronizationCollectorImpl();
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( sync );

		transactionCoordinator.getTransactionDriverControl().begin();
		assertEquals( 0, sync.getBeforeCompletionCount() );
		assertEquals( 0, sync.getSuccessfulCompletionCount() );
		assertEquals( 0, sync.getFailedCompletionCount() );

		transactionCoordinator.getTransactionDriverControl().commit();
		assertEquals( 1, sync.getBeforeCompletionCount() );
		assertEquals( 1, sync.getSuccessfulCompletionCount() );
		assertEquals( 0, sync.getFailedCompletionCount() );

	}

	@Test
	@SuppressWarnings("EmptyCatchBlock")
	public void testMarkRollbackOnly() {
		final TransactionCoordinatorOwnerTestingImpl owner = new TransactionCoordinatorOwnerTestingImpl();
		final JdbcResourceLocalTransactionCoordinatorBuilderImpl transactionCoordinatorBuilder =
				new JdbcResourceLocalTransactionCoordinatorBuilderImpl();

		final TransactionCoordinator transactionCoordinator = transactionCoordinatorBuilder.buildTransactionCoordinator(
				owner,
				new TransactionCoordinatorBuilder.Options() {
					@Override
					public boolean shouldAutoJoinTransaction() {
						return false;
					}
				}
		);

		assertEquals( TransactionStatus.NOT_ACTIVE, transactionCoordinator.getTransactionDriverControl().getStatus() );

		transactionCoordinator.getTransactionDriverControl().begin();
		assertEquals( TransactionStatus.ACTIVE, transactionCoordinator.getTransactionDriverControl().getStatus() );

		transactionCoordinator.getTransactionDriverControl().markRollbackOnly();
		assertEquals( TransactionStatus.MARKED_ROLLBACK, transactionCoordinator.getTransactionDriverControl().getStatus() );

		try {
			transactionCoordinator.getTransactionDriverControl().commit();
		}
		catch (TransactionException expected) {
		}
		finally {
			assertEquals( TransactionStatus.NOT_ACTIVE, transactionCoordinator.getTransactionDriverControl().getStatus() );
		}
	}

	@Test
	@SuppressWarnings("EmptyCatchBlock")
	public void testSynchronizationFailure() {
		final TransactionCoordinatorOwnerTestingImpl owner = new TransactionCoordinatorOwnerTestingImpl();
		final JdbcResourceLocalTransactionCoordinatorBuilderImpl transactionCoordinatorBuilder =
				new JdbcResourceLocalTransactionCoordinatorBuilderImpl();

		final TransactionCoordinator transactionCoordinator = transactionCoordinatorBuilder.buildTransactionCoordinator(
				owner,
				new TransactionCoordinatorBuilder.Options() {
					@Override
					public boolean shouldAutoJoinTransaction() {
						return false;
					}
				}
		);

		assertEquals( TransactionStatus.NOT_ACTIVE, transactionCoordinator.getTransactionDriverControl().getStatus() );
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( SynchronizationErrorImpl.forBefore() );

		transactionCoordinator.getTransactionDriverControl().begin();
		assertEquals( TransactionStatus.ACTIVE, transactionCoordinator.getTransactionDriverControl().getStatus() );

		try {
			transactionCoordinator.getTransactionDriverControl().commit();
		}
		catch (Exception expected) {
		}
		finally {
			assertEquals(
					TransactionStatus.NOT_ACTIVE,
					transactionCoordinator.getTransactionDriverControl().getStatus()
			);
		}
	}
}
