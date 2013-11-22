/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.transaction.synchronization.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Extension of SynchronizationCallbackCoordinatorNonTrackingImpl that adds checking of whether a rollback comes from
 * a thread other than the application thread (thread used to register the Synchronization)
 * 
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class SynchronizationCallbackCoordinatorTrackingImpl extends SynchronizationCallbackCoordinatorNonTrackingImpl {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( SynchronizationCallbackCoordinatorTrackingImpl.class );

	// magic numbers :(
	private static final int NO_STATUS = -1;

	private volatile long registrationThreadId;
	private volatile int delayedCompletionHandlingStatus;

	public SynchronizationCallbackCoordinatorTrackingImpl(TransactionCoordinator transactionCoordinator) {
		// super ctor calls reset() followed by pulse()
		super( transactionCoordinator );
	}

	@Override
	public void reset() {
		super.reset();
		// NOTE : reset is typically called:
		// 		1) on initialization, and
		// 		2) after "after completion" handling is finished.
		//
		// Here we use that to "clear out" all 'delayed after-completion" state.
		delayedCompletionHandlingStatus = NO_STATUS;
	}

	@Override
	public void afterCompletion(int status) {
		// The whole concept of "tracking" comes down to this code block..
		// Essentially we need to see if we can process the callback immediately.  So here we check whether the
		// current call is happening on the same thread as the thread under which we registered the Synchronization.
		// As far as we know, this can only ever happen in the rollback case where the transaction had been rolled
		// back on a separate "reaper" thread.  Since we know the transaction status and that check is not as heavy
		// as accessing the current thread, we check that first
		if ( JtaStatusHelper.isRollback( status ) ) {
			// we are processing a rollback, see if it is the same thread
			final long currentThreadId = Thread.currentThread().getId();
			final boolean isRegistrationThread = currentThreadId == registrationThreadId;
			if ( ! isRegistrationThread ) {
				// so we do have the condition of a rollback initiated from a separate thread.  Set the flag here and
				// check for it in SessionImpl. See HHH-7910.
				delayedCompletionHandlingStatus = status;

				log.rollbackFromBackgroundThread( status );
				return;
			}
		}

		// otherwise, do the callback immediately
		doAfterCompletion( status );
	}

	@Override
	public void synchronizationRegistered() {
		registrationThreadId = Thread.currentThread().getId();
	}

	@Override
	public void processAnyDelayedAfterCompletion() {
		if ( delayedCompletionHandlingStatus != NO_STATUS ) {
			doAfterCompletion( delayedCompletionHandlingStatus );
			delayedCompletionHandlingStatus = NO_STATUS;
			throw new HibernateException("Transaction was rolled back in a different thread!");
		}
	}
}
