/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction.jta;

import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import org.hibernate.TransactionException;
import org.hibernate.orm.test.resource.common.SynchronizationCollectorImpl;
import org.hibernate.orm.test.resource.common.SynchronizationErrorImpl;
import org.hibernate.resource.transaction.TransactionRequiredForJoinException;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinatorTrackingImpl;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBasicJtaTestScenarios {
	private final TransactionCoordinatorOwnerTestingImpl owner = new TransactionCoordinatorOwnerTestingImpl();
	private JtaTransactionCoordinatorBuilderImpl transactionCoordinatorBuilder = new JtaTransactionCoordinatorBuilderImpl();

	protected abstract boolean preferUserTransactions();

	public JtaTransactionCoordinatorImpl buildTransactionCoordinator(boolean autoJoin) {
		return new JtaTransactionCoordinatorImpl(
				transactionCoordinatorBuilder,
				owner,
				autoJoin,
				JtaPlatformStandardTestingImpl.INSTANCE,
				preferUserTransactions(),
				false
		);
	}

	@BeforeEach
	@AfterEach
	public void resetJtaPlatform() throws SystemException {
		// make sure the JTA platform is reset back to no active transactions before and after each test
		JtaPlatformStandardTestingImpl.INSTANCE.reset();
	}

	@Test
	public void basicBmtUsageTest() throws Exception {
		final JtaTransactionCoordinatorImpl transactionCoordinator = buildTransactionCoordinator( true );

		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );

		// begin the transaction
		transactionCoordinator.getTransactionDriverControl().begin();
		assertEquals( Status.STATUS_ACTIVE, tm.getStatus() );
		assertTrue( transactionCoordinator.isSynchronizationRegistered() );

		// create and add a local Synchronization
		SynchronizationCollectorImpl localSync = new SynchronizationCollectorImpl();
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( localSync );

		// commit the transaction
		transactionCoordinator.getTransactionDriverControl().commit();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertEquals( 1, localSync.getBeforeCompletionCount() );
		assertEquals( 1, localSync.getSuccessfulCompletionCount() );
		assertEquals( 0, localSync.getFailedCompletionCount() );
	}

	@Test
	public void rollbackBmtUsageTest() throws Exception {
		final JtaTransactionCoordinatorImpl transactionCoordinator = buildTransactionCoordinator( true );

		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );

		// begin the transaction
		transactionCoordinator.getTransactionDriverControl().begin();
		assertEquals( Status.STATUS_ACTIVE, tm.getStatus() );
		assertTrue( transactionCoordinator.isSynchronizationRegistered() );

		// create and add a local Synchronization
		SynchronizationCollectorImpl localSync = new SynchronizationCollectorImpl();
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( localSync );

		// rollback the transaction
		transactionCoordinator.getTransactionDriverControl().rollback();

		// post conditions
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertEquals( 0, localSync.getBeforeCompletionCount() );
		assertEquals( 0, localSync.getSuccessfulCompletionCount() );
		assertEquals( 1, localSync.getFailedCompletionCount() );
	}

	@Test
	public void basicCmtUsageTest() throws Exception {
		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		// begin the transaction
		tm.begin();

		final JtaTransactionCoordinatorImpl transactionCoordinator = buildTransactionCoordinator( true );
		assertEquals( Status.STATUS_ACTIVE, tm.getStatus() );
		// NOTE : because of auto-join
		assertTrue( transactionCoordinator.isSynchronizationRegistered() );

		// create and add a local Synchronization
		SynchronizationCollectorImpl localSync = new SynchronizationCollectorImpl();
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( localSync );

		// commit the transaction
		tm.commit();

		// post conditions
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertEquals( 1, localSync.getBeforeCompletionCount() );
		assertEquals( 1, localSync.getSuccessfulCompletionCount() );
		assertEquals( 0, localSync.getFailedCompletionCount() );
	}

	@Test
	public void basicCmtUsageWithPulseTest() throws Exception {
		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		final JtaTransactionCoordinatorImpl transactionCoordinator = buildTransactionCoordinator( true );

		// begin the transaction
		tm.begin();
		assertEquals( Status.STATUS_ACTIVE, tm.getStatus() );

		transactionCoordinator.pulse();
		// NOTE : because of auto-join
		assertTrue( transactionCoordinator.isSynchronizationRegistered() );
		transactionCoordinator.pulse();
		assertTrue( transactionCoordinator.isSynchronizationRegistered() );

		// create and add a local Synchronization
		SynchronizationCollectorImpl localSync = new SynchronizationCollectorImpl();
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( localSync );

		// commit the transaction
		tm.commit();

		// post conditions
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertEquals( 1, localSync.getBeforeCompletionCount() );
		assertEquals( 1, localSync.getSuccessfulCompletionCount() );
		assertEquals( 0, localSync.getFailedCompletionCount() );
	}

	@Test
	public void rollbackCmtUsageTest() throws Exception {
		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		// begin the transaction
		tm.begin();
		assertEquals( Status.STATUS_ACTIVE, tm.getStatus() );

		final JtaTransactionCoordinatorImpl transactionCoordinator = buildTransactionCoordinator( true );
		// NOTE : because of auto-join
		assertTrue( transactionCoordinator.isSynchronizationRegistered() );

		// create and add a local Synchronization
		SynchronizationCollectorImpl localSync = new SynchronizationCollectorImpl();
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( localSync );

		// rollback the transaction
		tm.rollback();

		// post conditions
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertEquals( 0, localSync.getBeforeCompletionCount() );
		assertEquals( 0, localSync.getSuccessfulCompletionCount() );
		assertEquals( 1, localSync.getFailedCompletionCount() );
	}

	@Test
	public void explicitJoiningTest() throws Exception {
		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		final JtaTransactionCoordinatorImpl transactionCoordinator = buildTransactionCoordinator( false );

		// begin the transaction
		tm.begin();

		assertEquals( Status.STATUS_ACTIVE, tm.getStatus() );

		// no auto-join now
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		transactionCoordinator.explicitJoin();
		assertTrue( transactionCoordinator.isSynchronizationRegistered() );

		// create and add a local Synchronization
		SynchronizationCollectorImpl localSync = new SynchronizationCollectorImpl();
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( localSync );

		// commit the transaction
		tm.commit();

		// post conditions
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertEquals( 1, localSync.getBeforeCompletionCount() );
		assertEquals( 1, localSync.getSuccessfulCompletionCount() );
		assertEquals( 0, localSync.getFailedCompletionCount() );
	}

	@Test
	public void jpaExplicitJoiningTest() throws Exception {
		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		// begin the transaction
		tm.begin();

		assertEquals( Status.STATUS_ACTIVE, tm.getStatus() );

		final JtaTransactionCoordinatorImpl transactionCoordinator = buildTransactionCoordinator( false );
		// no auto-join now
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		transactionCoordinator.explicitJoin();
		assertTrue( transactionCoordinator.isSynchronizationRegistered() );

		// create and add a local Synchronization
		SynchronizationCollectorImpl localSync = new SynchronizationCollectorImpl();
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( localSync );

		// commit the transaction
		tm.commit();

		// post conditions
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertEquals( 1, localSync.getBeforeCompletionCount() );
		assertEquals( 1, localSync.getSuccessfulCompletionCount() );
		assertEquals( 0, localSync.getFailedCompletionCount() );
	}

	@Test
	public void assureMultipleJoinCallsNoOp() throws Exception {
		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		// begin the transaction
		tm.begin();

		assertEquals( Status.STATUS_ACTIVE, tm.getStatus() );

		final JtaTransactionCoordinatorImpl transactionCoordinator = buildTransactionCoordinator( false );
		// no auto-join now
		assertFalse( transactionCoordinator.isJoined() );
		transactionCoordinator.explicitJoin();
		assertTrue( transactionCoordinator.isJoined() );
		transactionCoordinator.explicitJoin();
		transactionCoordinator.explicitJoin();
		transactionCoordinator.explicitJoin();
		transactionCoordinator.explicitJoin();
		assertTrue( transactionCoordinator.isJoined() );

		// create and add a local Synchronization
		SynchronizationCollectorImpl localSync = new SynchronizationCollectorImpl();
		transactionCoordinator.getLocalSynchronizations().registerSynchronization( localSync );

		// commit the transaction
		tm.commit();

		// post conditions
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );
		assertFalse( transactionCoordinator.isSynchronizationRegistered() );
		assertEquals( 1, localSync.getBeforeCompletionCount() );
		assertEquals( 1, localSync.getSuccessfulCompletionCount() );
		assertEquals( 0, localSync.getFailedCompletionCount() );
	}

	@Test
	@SuppressWarnings("EmptyCatchBlock")
	public void explicitJoinOutsideTxnTest() throws Exception {
		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		final JtaTransactionCoordinatorImpl transactionCoordinator = buildTransactionCoordinator( false );

		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		// try to force a join, should fail...
		try {
			transactionCoordinator.explicitJoin();
			fail( "Expecting explicitJoin() call outside of transaction to fail" );
		}
		catch (TransactionRequiredForJoinException expected) {
		}
	}

	@Test
	public void basicThreadCheckingUsage() throws Exception {
		JtaTransactionCoordinatorImpl transactionCoordinator = new JtaTransactionCoordinatorImpl(
				transactionCoordinatorBuilder,
				owner,
				true,
				JtaPlatformStandardTestingImpl.INSTANCE,
				preferUserTransactions(),
				true
		);

		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		// begin the transaction
		tm.begin();
		transactionCoordinator.explicitJoin();
		assertEquals(
				SynchronizationCallbackCoordinatorTrackingImpl.class,
				transactionCoordinator.getSynchronizationCallbackCoordinator().getClass()
		);

		tm.commit();


		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );
		assertFalse( transactionCoordinator.isJoined() );

		tm.begin();
		transactionCoordinator.explicitJoin();
		assertEquals(
				SynchronizationCallbackCoordinatorTrackingImpl.class,
				transactionCoordinator.getSynchronizationCallbackCoordinator().getClass()
		);

		tm.rollback();
	}

	@Test
	@SuppressWarnings("EmptyCatchBlock")
	public void testMarkRollbackOnly() throws Exception {
		JtaTransactionCoordinatorImpl transactionCoordinator = new JtaTransactionCoordinatorImpl(
				transactionCoordinatorBuilder,
				owner,
				true,
				JtaPlatformStandardTestingImpl.INSTANCE,
				preferUserTransactions(),
				true
		);

		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

		assertEquals( TransactionStatus.NOT_ACTIVE, transactionCoordinator.getTransactionDriverControl().getStatus() );

		transactionCoordinator.getTransactionDriverControl().begin();
		assertEquals( TransactionStatus.ACTIVE, transactionCoordinator.getTransactionDriverControl().getStatus() );

		transactionCoordinator.getTransactionDriverControl().markRollbackOnly();
		assertEquals(
				TransactionStatus.MARKED_ROLLBACK,
				transactionCoordinator.getTransactionDriverControl().getStatus()
		);

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
	public void testSynchronizationFailure() throws Exception {
		JtaTransactionCoordinatorImpl transactionCoordinator = new JtaTransactionCoordinatorImpl(
				transactionCoordinatorBuilder,
				owner,
				true,
				JtaPlatformStandardTestingImpl.INSTANCE,
				preferUserTransactions(),
				true
		);

		// pre conditions
		final TransactionManager tm = JtaPlatformStandardTestingImpl.INSTANCE.transactionManager();
		assertEquals( Status.STATUS_NO_TRANSACTION, tm.getStatus() );

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
			assertEquals( TransactionStatus.NOT_ACTIVE, transactionCoordinator.getTransactionDriverControl().getStatus() );
		}
	}
}
