/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.internal.CoreMessageLogger;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * Extension of SynchronizationCallbackCoordinatorNonTrackingImpl that adds checking of whether a rollback comes from
 * a thread other than the application thread (thread used to register the Synchronization)
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class SynchronizationCallbackCoordinatorTrackingImpl extends SynchronizationCallbackCoordinatorNonTrackingImpl {
	private static final CoreMessageLogger log = messageLogger( SynchronizationCallbackCoordinatorTrackingImpl.class );

	private volatile long registrationThreadId;
	private volatile boolean delayedCompletionHandling;

	public SynchronizationCallbackCoordinatorTrackingImpl(SynchronizationCallbackTarget target) {
		// super ctor calls reset() followed by pulse()
		super( target );
	}

	@Override
	public void reset() {
		super.reset();
		// NOTE : reset is typically called:
		// 		1) on initialization, and
		// 		2) afterQuery "afterQuery completion" handling is finished.
		//
		// Here we use that to "clear out" all 'delayed afterQuery-completion" state.  The registrationThreadId will
		// "lazily" be re-populated on the next synchronizationRegistered call to allow for the potential of the
		// next Session transaction occurring on a different thread (though that transaction would need to completely
		// operate on that thread).
		delayedCompletionHandling = false;
	}

	@Override
	public void afterCompletion(int status) {
		log.tracef( "Synchronization coordinator: afterCompletion(status=%s)", status );

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
				delayedCompletionHandling = true;

				log.rollbackFromBackgroundThread( status );
				return;
			}
		}

		// otherwise, do the callback immediately
		doAfterCompletion( JtaStatusHelper.isCommitted( status ), false );
	}

	@Override
	public void synchronizationRegistered() {
		registrationThreadId = Thread.currentThread().getId();
	}

	@Override
	public void processAnyDelayedAfterCompletion() {
		if ( delayedCompletionHandling ) {
			delayedCompletionHandling = false;

			// false here (rather than how we used to keep and check the status) because as discussed above
			// the delayed logic should only ever occur during rollback
			doAfterCompletion( false, true );

			// NOTE : doAfterCompletion calls reset
			throw new HibernateException( "Transaction was rolled back in a different thread!" );
		}
	}
}
