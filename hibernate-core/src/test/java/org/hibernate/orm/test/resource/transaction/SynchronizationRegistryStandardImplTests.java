/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.resource.transaction;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

import org.hibernate.orm.test.resource.common.SynchronizationCollectorImpl;
import org.hibernate.orm.test.resource.common.SynchronizationErrorImpl;
import org.hibernate.resource.transaction.LocalSynchronizationException;
import org.hibernate.resource.transaction.NullSynchronizationException;
import org.hibernate.resource.transaction.internal.SynchronizationRegistryStandardImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for SynchronizationRegistryStandardImpl.
 *
 * @author Steve Ebersole
 */
public class SynchronizationRegistryStandardImplTests {
	@Test
	public void basicUsageTests() {
		final SynchronizationRegistryStandardImpl registry = new SynchronizationRegistryStandardImpl();
		try {
			registry.registerSynchronization( null );
			fail( "Was expecting NullSynchronizationException, but call succeeded" );
		}
		catch (NullSynchronizationException expected) {
			// expected behavior
		}
		catch (Exception e) {
			fail( "Was expecting NullSynchronizationException, but got " + e.getClass().getName() );
		}

		final SynchronizationCollectorImpl synchronization = new SynchronizationCollectorImpl();
		assertEquals( 0, registry.getNumberOfRegisteredSynchronizations() );
		registry.registerSynchronization( synchronization );
		assertEquals( 1, registry.getNumberOfRegisteredSynchronizations() );
		registry.registerSynchronization( synchronization );
		assertEquals( 1, registry.getNumberOfRegisteredSynchronizations() );

		assertEquals( 0, synchronization.getBeforeCompletionCount() );
		assertEquals( 0, synchronization.getSuccessfulCompletionCount() );
		assertEquals( 0, synchronization.getFailedCompletionCount() );

		{
			registry.notifySynchronizationsBeforeTransactionCompletion();
			assertEquals( 1, synchronization.getBeforeCompletionCount() );
			assertEquals( 0, synchronization.getSuccessfulCompletionCount() );
			assertEquals( 0, synchronization.getFailedCompletionCount() );

			registry.notifySynchronizationsAfterTransactionCompletion( Status.STATUS_COMMITTED );
			assertEquals( 1, synchronization.getBeforeCompletionCount() );
			assertEquals( 1, synchronization.getSuccessfulCompletionCount() );
			assertEquals( 0, synchronization.getFailedCompletionCount() );
		}

		// after completion should clear registered synchronizations
		assertEquals( 0, registry.getNumberOfRegisteredSynchronizations() );
		// reset the sync
		synchronization.reset();
		assertEquals( 0, synchronization.getBeforeCompletionCount() );
		assertEquals( 0, synchronization.getSuccessfulCompletionCount() );
		assertEquals( 0, synchronization.getFailedCompletionCount() );
		// re-register it
		registry.registerSynchronization( synchronization );
		assertEquals( 1, registry.getNumberOfRegisteredSynchronizations() );

		{
			registry.notifySynchronizationsAfterTransactionCompletion( Status.STATUS_ROLLEDBACK );
			assertEquals( 0, synchronization.getBeforeCompletionCount() );
			assertEquals( 0, synchronization.getSuccessfulCompletionCount() );
			assertEquals( 1, synchronization.getFailedCompletionCount() );

			// after completion should clear registered synchronizations
			assertEquals( 0, registry.getNumberOfRegisteredSynchronizations() );
		}
	}

	@Test
	public void testUserSynchronizationExceptions() {
		// exception in beforeCompletion
		SynchronizationRegistryStandardImpl registry = new SynchronizationRegistryStandardImpl();
		Synchronization synchronization = SynchronizationErrorImpl.forBefore();
		registry.registerSynchronization( synchronization );
		try {
			registry.notifySynchronizationsBeforeTransactionCompletion();
			fail( "Expecting LocalSynchronizationException, but call succeeded" );
		}
		catch (LocalSynchronizationException expected) {
			// expected
		}
		catch (Exception e) {
			fail( "Was expecting LocalSynchronizationException, but got " + e.getClass().getName() );
		}


		// exception in beforeCompletion
		registry.clearSynchronizations();
		registry = new SynchronizationRegistryStandardImpl();
		synchronization = SynchronizationErrorImpl.forAfter();
		registry.registerSynchronization( synchronization );
		try {
			registry.notifySynchronizationsAfterTransactionCompletion( Status.STATUS_COMMITTED );
			fail( "Expecting LocalSynchronizationException, but call succeeded" );
		}
		catch (LocalSynchronizationException expected) {
			// expected
		}
		catch (Exception e) {
			fail( "Was expecting LocalSynchronizationException, but got " + e.getClass().getName() );
		}

	}
}
