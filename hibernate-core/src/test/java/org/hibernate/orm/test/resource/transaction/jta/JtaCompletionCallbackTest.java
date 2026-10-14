package org.hibernate.orm.test.resource.transaction.jta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinator;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinatorNonTrackingImpl;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackCoordinatorTrackingImpl;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.SynchronizationCallbackTarget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies that JTA completion preserves status for local synchronizations,
/// including callbacks deferred from a background rollback thread.
///
/// @author Steve Ebersole
class JtaCompletionCallbackTest {

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void immediateCompletionPreservesStatus(boolean trackByThread) {
		var owner = new TransactionCoordinatorOwnerTestingImpl();
		var coordinator = coordinator( owner, trackByThread );
		var callbacks = coordinator.getSynchronizationCallbackCoordinator();
		List<Integer> statuses = new ArrayList<>();
		for ( int status : new int[] { Status.STATUS_COMMITTED, Status.STATUS_ROLLEDBACK, Status.STATUS_UNKNOWN } ) {
			callbacks.synchronizationRegistered();
			collectStatuses( coordinator, statuses );
			callbacks.afterCompletion( status );
		}
		assertEquals( List.of( Status.STATUS_COMMITTED, Status.STATUS_ROLLEDBACK, Status.STATUS_UNKNOWN ), statuses );
		assertEquals( 1, owner.getSuccessfulCompletionCount() );
		assertEquals( 2, owner.getFailedCompletionCount() );
	}

	@Test
	void deferredRollbackPreservesStatusAndRunsOnce() throws InterruptedException {
		List<Boolean> delayedCalls = new ArrayList<>();
		var owner = new TransactionCoordinatorOwnerTestingImpl() {
			@Override
			public void afterTransactionCompletion(boolean successful, boolean delayed) {
				super.afterTransactionCompletion( successful, delayed );
				delayedCalls.add( delayed );
			}
		};
		var coordinator = coordinator( owner, true );
		var callbacks = coordinator.getSynchronizationCallbackCoordinator();
		List<Integer> statuses = new ArrayList<>();
		List<Thread> deliveryThreads = new ArrayList<>();
		callbacks.synchronizationRegistered();
		coordinator.getLocalSynchronizations().registerSynchronization( new Synchronization() {
			@Override
			public void beforeCompletion() {
			}

			@Override
			public void afterCompletion(int status) {
				statuses.add( status );
				deliveryThreads.add( Thread.currentThread() );
			}
		} );
		AtomicReference<Throwable> backgroundFailure = new AtomicReference<>();
		Thread background = new Thread( () -> {
			try {
				callbacks.afterCompletion( Status.STATUS_ROLLEDBACK );
			}
			catch (Throwable failure) {
				backgroundFailure.set( failure );
			}
		} );
		background.start();
		background.join();
		assertNull( backgroundFailure.get() );
		assertTrue( statuses.isEmpty() );
		assertEquals( 0, owner.getFailedCompletionCount() );

		assertThrows( HibernateException.class, callbacks::processAnyDelayedAfterCompletion );
		assertEquals( List.of( Status.STATUS_ROLLEDBACK ), statuses );
		assertEquals( List.of( Thread.currentThread() ), deliveryThreads );
		assertEquals( 1, owner.getFailedCompletionCount() );
		assertEquals( List.of( true ), delayedCalls );
		callbacks.processAnyDelayedAfterCompletion();
		assertEquals( List.of( Status.STATUS_ROLLEDBACK ), statuses );

		// A subsequent transaction must not replay the deferred rollback or retain its status.
		callbacks.synchronizationRegistered();
		collectStatuses( coordinator, statuses );
		callbacks.afterCompletion( Status.STATUS_COMMITTED );
		callbacks.processAnyDelayedAfterCompletion();
		assertEquals( List.of( Status.STATUS_ROLLEDBACK, Status.STATUS_COMMITTED ), statuses );
		assertEquals( 1, owner.getSuccessfulCompletionCount() );
		assertEquals( 1, owner.getFailedCompletionCount() );
		assertEquals( List.of( true, false ), delayedCalls );
	}

	@Test
	void legacyBooleanCompletionDoesNotInventRollback() {
		var coordinator = coordinator( new TransactionCoordinatorOwnerTestingImpl(), false );
		List<Integer> statuses = new ArrayList<>();
		collectStatuses( coordinator, statuses );
		coordinator.afterCompletion( false, false );
		collectStatuses( coordinator, statuses );
		coordinator.afterCompletion( true, false );
		assertEquals( List.of( Status.STATUS_UNKNOWN, Status.STATUS_COMMITTED ), statuses );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void booleanOnlyTargetsRemainCompatible(boolean trackByThread) {
		List<Boolean> successful = new ArrayList<>();
		SynchronizationCallbackTarget target = new SynchronizationCallbackTarget() {
			@Override
			public boolean isActive() {
				return true;
			}

			@Override
			public void beforeCompletion() {
			}

			@Override
			public void afterCompletion(boolean committed, boolean delayed) {
				successful.add( committed );
			}
		};
		SynchronizationCallbackCoordinator callbacks = trackByThread
				? new SynchronizationCallbackCoordinatorTrackingImpl( target )
				: new SynchronizationCallbackCoordinatorNonTrackingImpl( target );
		for ( int status : new int[] { Status.STATUS_COMMITTED, Status.STATUS_ROLLEDBACK, Status.STATUS_UNKNOWN } ) {
			callbacks.synchronizationRegistered();
			callbacks.afterCompletion( status );
		}
		assertEquals( List.of( true, false, false ), successful );
	}

	private JtaTransactionCoordinatorImpl coordinator(TransactionCoordinatorOwnerTestingImpl owner, boolean tracking) {
		return new JtaTransactionCoordinatorImpl( new JtaTransactionCoordinatorBuilderImpl(), owner, false,
				NoJtaPlatform.INSTANCE, false, tracking );
	}

	private void collectStatuses(JtaTransactionCoordinatorImpl coordinator, List<Integer> statuses) {
		coordinator.getLocalSynchronizations().registerSynchronization( new Synchronization() {
			@Override
			public void beforeCompletion() {
			}

			@Override
			public void afterCompletion(int status) {
				statuses.add( status );
			}
		} );
	}
}
